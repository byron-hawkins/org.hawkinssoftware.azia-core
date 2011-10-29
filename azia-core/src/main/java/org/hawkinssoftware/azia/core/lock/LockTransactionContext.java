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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkinssoftware.azia.core.action.LayoutTransaction;
import org.hawkinssoftware.azia.core.action.TransactionRegistry;
import org.hawkinssoftware.azia.core.action.UserInterfaceActor;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.CollisionStatus;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.collection.AccessValidatingSet;
import org.hawkinssoftware.rns.core.lock.HookSemaphores;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.validation.ValidateRead;
import org.hawkinssoftware.rns.core.validation.ValidateWrite;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@ExecutionPath.NoFrame
@HookSemaphores(hook = LockAccessValidator.class, instance = "getInstance()")
@DomainRole.Join(membership = { LockManagement.class, TransactionFacilitation.class })
class LockTransactionContext
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	enum LockState
	{
		OK,
		COLLISION;
	}

	// this class wrapper isolates the contexts per thread and their access from the @HookSemaphores instrumentation of
	// the containing class
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	static class PerThread
	{
		static ThreadLocal<LockTransactionContext> CONTEXTS = new ThreadLocal<LockTransactionContext>() {
			@Override
			protected LockTransactionContext initialValue()
			{
				LockTransactionContext newContext = new LockTransactionContext();
				synchronized (ALL)
				{
					ALL.add(newContext);
				}
				return newContext;
			}
		};

		// locked under itself
		static final List<LockTransactionContext> ALL = new ArrayList<LockTransactionContext>();

		static Collection<LockTransactionContext> getAll()
		{
			List<LockTransactionContext> all = new ArrayList<LockTransactionContext>();
			synchronized (ALL)
			{
				all.addAll(ALL);
			}
			return all;
		}
	}

	static LockTransactionContext get()
	{
		return PerThread.CONTEXTS.get();
	}

	final Thread thread;

	// not under lock contention
	final Map<LayoutTransaction, UserInterfaceLock> layoutRootLocksByTransaction = new HashMap<LayoutTransaction, UserInterfaceLock>();
	// this exists for the case of moving a layout entity under a different root--not sure it's necessary (could
	// just create new)
	final Map<UserInterfaceActor, UserInterfaceLock> adjustingLocksByActor = new HashMap<UserInterfaceActor, UserInterfaceLock>();

	final Set<UserInterfaceActor> actorsInstantiatedThisTransaction = new HashSet<UserInterfaceActor>();

	/**
	 * locked under LockTransactionContext.class with special conditions:
	 * 
	 * <pre>
	 * 1. The owner may read without lock, because the owner is the only writer, and ownership is per thread
	 * 2. The owner must lock before writing
	 * 3. The checkCrossLock() method must lock before reading, because it operates on all contexts at once
	 * </pre>
	 */
	final Set<AutonomousLock> readOnlyLocks = AccessValidatingSet.create(new HashSet<AutonomousLock>(),
			new LockAccessValidator.LockTransactionContextSetValidator<AutonomousLock>("readOnlyLocks"));
	// ditto
	final Set<AutonomousLock> fullLocks = AccessValidatingSet.create(new HashSet<AutonomousLock>(),
			new LockAccessValidator.LockTransactionContextSetValidator<AutonomousLock>("fullLocks"));

	// locked under LockTransactionContext.class
	@ValidateWrite(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldWrite")
	@ValidateRead(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldRead")
	AutonomousLock awaitedLock = null;

	// locked under LockTransactionContext.class
	@ValidateWrite(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldWrite")
	@ValidateRead(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldRead")
	boolean transactionActive = false;

	// locked under LockTransactionContext.class (misread is possible but not a problem)
	@ValidateWrite(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldWrite")
	@ValidateRead(validatorType = LockAccessValidator.FieldAccessValidator.class, method = "validateLockTransactionContextFieldRead")
	volatile boolean interrupted = false;

	final LockAccessValidator semaphoreAccessValidator = new LockAccessValidator(this);

	private LockTransactionContext()
	{
		this.thread = Thread.currentThread();
	}

	private void safeCheckInterrupted()
	{
		if (interrupted)
		{
			TransactionRegistry.failCurrentTask(CollisionStatus.CROSSLOCK);
		}
	}

	void checkInterrupted()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
		}
	}

	void beginSession()
	{
		synchronized (LockTransactionContext.class)
		{
			interrupted = false;
			awaitedLock = null;
		}
	}

	void addReadOnlyLock(AutonomousLock lock)
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			readOnlyLocks.add(lock);
		}
	}

	Set<AutonomousLock> getReadOnlyLocks()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			return readOnlyLocks;
		}
	}

	void addFullLock(AutonomousLock lock)
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			fullLocks.add(lock);
		}
	}

	Set<AutonomousLock> getFullLocks()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			return new HashSet<AutonomousLock>(fullLocks);
		}
	}

	boolean hasFullLock(AutonomousLock lock)
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			return fullLocks.contains(lock);
		}
	}

	void clearLocks()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			readOnlyLocks.clear();
			fullLocks.clear();
		}
	}

	Collection<AutonomousLock> emergencyClearAllLocks()
	{
		synchronized (LockTransactionContext.class)
		{
			Set<AutonomousLock> locks = new HashSet<AutonomousLock>();
			locks.addAll(fullLocks);
			locks.addAll(readOnlyLocks);
			fullLocks.clear();
			readOnlyLocks.clear();
			transactionActive = false;
			return locks;
		}
	}

	boolean isTransactionActive()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			return transactionActive;
		}
	}

	void setTransactionActive(boolean transactionActive)
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			this.transactionActive = transactionActive;
		}
	}

	boolean isWaiting()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			return awaitedLock != null;
		}
	}

	LockState setAwaitedLock(AutonomousLock awaitedLock)
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			this.awaitedLock = awaitedLock;
			return CrossLockValidator.checkCrossLock(awaitedLock);
		}
	}

	void clearAwaitedLock()
	{
		synchronized (LockTransactionContext.class)
		{
			safeCheckInterrupted();
			this.awaitedLock = null;
		}
	}
}
