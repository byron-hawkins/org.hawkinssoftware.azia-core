/*
 * Copyright (c) 2011 HawkinsSoftware
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Byron Hawkins of HawkinsSoftware
 */
package org.hawkinssoftware.azia.core.lock;

import java.util.HashMap;
import java.util.Map;

import org.hawkinssoftware.azia.core.action.LayoutTransaction;
import org.hawkinssoftware.azia.core.action.TransactionRegistry;
import org.hawkinssoftware.azia.core.action.UserInterfaceActor;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.CollisionStatus;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransaction;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.layout.BoundedEntity;
import org.hawkinssoftware.azia.core.lock.AutonomousLock.RequestMode;
import org.hawkinssoftware.azia.core.lock.AutonomousLock.Result;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.collection.AccessValidatingMap;
import org.hawkinssoftware.rns.core.lock.HookSemaphores;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.CoreDomains.InitializationDomain;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@ExecutionPath.NoFrame
@DomainRole.Join(membership = { LockManagement.class, TransactionFacilitation.class })
@HookSemaphores(hook = LockAccessValidator.class, instance = "getInstance()")
@InvocationConstraint(packages = { "org.hawkinssoftware.azia.core.lock.*", "org.hawkinssoftware.azia.core.action.*" })
public class LockRegistry
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	private static class InstantiationStack extends ThreadLocal<LockRegistrationStack>
	{
		@Override
		protected LockRegistrationStack initialValue()
		{
			return new LockRegistrationStack();
		}
	}

	private static final InstantiationStack INSTANTIATION_STACK = new InstantiationStack();

	// use `new String` to avoid intern collision on the monitor
	static Object adminLock = new String("LockRegistry's Admin Lock");

	private static LockRegistry INSTANCE;

	@InvocationConstraint(domains = InitializationDomain.class)
	public static void initialize()
	{ 
		INSTANCE = new LockRegistry();
		FieldAccessLockValidator.initialize();
	}

	public static LockRegistry getInstance()
	{
		return INSTANCE;
	}

	// synchronized under `adminLock
	final Map<UserInterfaceActor, UserInterfaceLock> locksByActor = AccessValidatingMap.create(new HashMap<UserInterfaceActor, UserInterfaceLock>(),
			new LockAccessValidator.LockRegistryMapValidator<UserInterfaceActor, UserInterfaceLock>("locksByActor"));

	// synchronized under `adminLock
	final Map<BoundedEntity.LayoutRoot, UserInterfaceLock> locksByLayoutRoot = AccessValidatingMap.create(
			new HashMap<BoundedEntity.LayoutRoot, UserInterfaceLock>(),
			new LockAccessValidator.LockRegistryMapValidator<BoundedEntity.LayoutRoot, UserInterfaceLock>("locksByLayoutRoot"));

	public void beginSession()
	{
		LockTransactionContext.get().beginSession();
	}

	public void registerActor(UserInterfaceActor actor, boolean isHandler)
	{
		LockRegistrationStack stack = INSTANTIATION_STACK.get();
		if (stack.isEmpty())
		{
			throw new IllegalStateException(
					"Attempt to register an actor with no lock on the registration stack. Please execute instantiation within an InstantiationTask.");
		}

		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		if (!context.isTransactionActive())
		{
			throw new IllegalStateException(
					"Attempt to register an actor with no transactions in progress. If nothing else is going on, please initiate a GenericTransaction before constructing actors.");
		}

		synchronized (adminLock)
		{
			if (isHandler)
			{
				locksByActor.put(actor, stack.attachDependent(actor.getClass().getSimpleName()));
			}
			else
			{
				locksByActor.put(actor, stack.peek());
			}
		}
		context.actorsInstantiatedThisTransaction.add(actor);
	}

	public void registerLayoutActor(UserInterfaceActor actor, LayoutTransaction transaction)
	{
		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		if (context.layoutRootLocksByTransaction.get(transaction) == null)
		{
			if (actor instanceof BoundedEntity.LayoutRoot)
			{
				beginLayoutTransaction(transaction, (BoundedEntity.LayoutRoot) actor);
			}
			else
			{
				System.err.println("Warning: attempt to register a layout actor with no layout root lock on the execution stack.");
				return;
			}
		}

		synchronized (adminLock)
		{
			UserInterfaceLock rootLock = context.layoutRootLocksByTransaction.get(transaction);
			locksByActor.put(actor, new DependentLock(rootLock, actor.getClass().getSimpleName()));
		}
		context.actorsInstantiatedThisTransaction.add(actor);
	}

	public void beginInstantiation(UserInterfaceActor.SynchronizationRole actorType, String description)
	{
		INSTANTIATION_STACK.get().push(actorType, description);
	}

	public void beginSubordinateInstantiation(UserInterfaceActor actor)
	{
		synchronized (adminLock)
		{
			INSTANTIATION_STACK.get().push(locksByActor.get(actor));
		}
	}

	public void endInstantiation()
	{
		INSTANTIATION_STACK.get().pop(); 
	}

	public void beginTransaction(UserInterfaceTransaction transaction)
	{
		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		context.setTransactionActive(true);

		// System.out.println("Begin " + transaction.getClass().getSimpleName());
	}

	public void beginLayoutTransaction(LayoutTransaction transaction)
	{
		beginLayoutTransaction(transaction, transaction.getLayoutRoot());
	}

	public void beginLayoutTransaction(LayoutTransaction transaction, BoundedEntity.LayoutRoot root)
	{
		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		context.setTransactionActive(true);

		// System.out.println("Begin assembly of layout change");

		if (root == null)
		{
			System.err.println("Warning: attempt to begin a layout transaction with no root in the transaction.");
			return;
		}

		synchronized (adminLock)
		{
			if (context.layoutRootLocksByTransaction.containsKey((LayoutTransaction) transaction))
			{
				throw new IllegalStateException("Duplicate attempt to begin a layout transaction.");
			}

			UserInterfaceLock rootLock = locksByLayoutRoot.get(root);
			if (rootLock == null)
			{
				rootLock = new AutonomousLock("Layout root");
				locksByLayoutRoot.put(root, rootLock);
			}
			context.layoutRootLocksByTransaction.put((LayoutTransaction) transaction, rootLock);
		}
	}

	// TODO: I think this should be a generic actor re-registration of some kind
	public void postLayoutChange(BoundedEntity layoutEntity)
	{
		// ExecutionContext context = CURRENT_CONTEXT.get();
		// context.adjustingLocksByActor.put(layoutEntity, context.layoutRootLock);
	}

	public void lockForAssembly(UserInterfaceTransaction transaction, UserInterfaceActor actor)
	{
		AutonomousLock lock;
		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		synchronized (adminLock)
		{
			lock = locksByActor.get(actor).getAutonomousLock();
		}
		if (context.hasFullLock(lock))
		{
			return;
		}

		// TODO: is it safe to modularize by lock method, maybe make it another enum?
		if (lock.assemblyLock(RequestMode.IMMEDIATE) == Result.BUSY)
		{
			switch (context.setAwaitedLock(lock))
			{
				case OK:
					switch (lock.assemblyLock(RequestMode.WAIT))
					{
						case INTERRUPTED:
							TransactionRegistry.failCurrentTask(CollisionStatus.CROSSLOCK);
						case TIMEOUT:
							TransactionRegistry.failCurrentTask(CollisionStatus.TIMEOUT);
					}
					context.clearAwaitedLock();
					break;
				case COLLISION:
					TransactionRegistry.failCurrentTask(CollisionStatus.CROSSLOCK);
			}
		}
		context.addFullLock(lock);
	}

	public void commitLock()
	{
		// System.out.println("Commit lock on thread " + Thread.currentThread().getName());

		LockTransactionContext context = LockTransactionContext.get();
		for (AutonomousLock lock : context.getFullLocks())
		{
			context.checkInterrupted();
			if (lock.commitLock(RequestMode.IMMEDIATE) == Result.BUSY)
			{
				switch (context.setAwaitedLock(lock))
				{
					case OK:
						switch (lock.commitLock(RequestMode.WAIT))
						{
							case INTERRUPTED:
								TransactionRegistry.failCurrentTask(CollisionStatus.CROSSLOCK);
							case TIMEOUT:
								TransactionRegistry.failCurrentTask(CollisionStatus.TIMEOUT);
						}
						context.clearAwaitedLock();
						break;
					case COLLISION:
						TransactionRegistry.failCurrentTask(CollisionStatus.CROSSLOCK);
				}
			}
		}
	}

	public void commitUnlock()
	{
		// System.out.println("Commit unlock on thread " + Thread.currentThread().getName());

		LockTransactionContext context = LockTransactionContext.get();
		synchronized (LockTransactionContext.class)
		{
			for (AutonomousLock lock : context.getFullLocks())
			{
				lock.commitUnlock();
			}
			for (AutonomousLock lock : context.getReadOnlyLocks())
			{
				lock.clearReadLocks();
			}
			context.clearLocks();
			context.setTransactionActive(false);
		}

		synchronized (adminLock)
		{
			locksByActor.putAll(context.adjustingLocksByActor);
		}
		context.layoutRootLocksByTransaction.clear();
		context.adjustingLocksByActor.clear();
		context.actorsInstantiatedThisTransaction.clear();
	}

	public void emergencyReleaseContext()
	{
		LockTransactionContext context = LockTransactionContext.get();
		synchronized (LockTransactionContext.class)
		{
			for (AutonomousLock lock : context.emergencyClearAllLocks())
			{
				lock.emergencyReleaseAllLock();
			}
		}

		context.layoutRootLocksByTransaction.clear();
		context.adjustingLocksByActor.clear();
		context.actorsInstantiatedThisTransaction.clear();
	}
}
