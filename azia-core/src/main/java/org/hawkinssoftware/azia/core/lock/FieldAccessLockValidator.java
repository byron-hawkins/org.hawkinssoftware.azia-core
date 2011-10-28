package org.hawkinssoftware.azia.core.lock;

import org.hawkinssoftware.azia.core.action.TransactionRegistry;
import org.hawkinssoftware.azia.core.action.UserInterfaceActor;
import org.hawkinssoftware.azia.core.action.UserInterfaceActorDelegate;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.CollisionStatus;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.lock.AutonomousLock.RequestMode;
import org.hawkinssoftware.azia.core.lock.AutonomousLock.Result;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.lock.HookSemaphores;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.CoreDomains.InitializationDomain;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.validation.ValidateRead;
import org.hawkinssoftware.rns.core.validation.ValidateWrite;

@ExecutionPath.NoFrame
@HookSemaphores(hook = LockAccessValidator.class, instance = "getInstance()")
@DomainRole.Join(membership = { LockManagement.class, TransactionFacilitation.class })
class FieldAccessLockValidator implements ValidateWrite.Validator, ValidateRead.Validator
{
	private static final FieldAccessLockValidator INSTANCE = new FieldAccessLockValidator();

	@InvocationConstraint(domains = InitializationDomain.class)
	static void initialize()
	{
		ValidateWrite.ValidationAgent.setValidator(INSTANCE);
		ValidateRead.ValidationAgent.setValidator(INSTANCE);
	}

	@Override
	@InvocationConstraint(types = ValidateRead.ValidationAgent.class)
	public void validateRead(Object reader, Object fieldOwner, String fieldName)
	{
		UserInterfaceActor actor;
		if (fieldOwner instanceof UserInterfaceActorDelegate)
		{
			actor = ((UserInterfaceActorDelegate) fieldOwner).getActor();
		}
		else if (fieldOwner instanceof UserInterfaceActor)
		{
			actor = (UserInterfaceActor) fieldOwner;
		}
		else
		{
			Log.out(Tag.LOCK_WARNING, "Warining: attempt to validate read with an entity that cannot be correlated to a responsible actor: %s", fieldOwner
					.getClass().getName());
			return;
		}

		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		if (context.actorsInstantiatedThisTransaction.contains(actor))
		{
			return;
		}

		if (!context.isTransactionActive())
		{
			Log.out(Tag.LOCK_WARNING,
					"Warning: attempt to read %s.%s with no transaction active. If no writes are required, please initiate a ReadOnlyTransaction.", fieldOwner
							.getClass().getSimpleName(), fieldName);
		}

		UserInterfaceLock lock = null;
		synchronized (LockRegistry.adminLock)
		{
			lock = LockRegistry.getInstance().locksByActor.get(actor);
		}
		if (lock == null)
		{
			Log.out(Tag.LOCK_WARNING, "Warining: attempt to validate read with an actor that has no registered lock: %s", fieldOwner.getClass().getName());
			return;
		}

		AutonomousLock physicalLock = lock.getAutonomousLock();
		if (physicalLock.hasReadPermission())
		{
			return;
		}

		if (physicalLock.readLock(RequestMode.IMMEDIATE) == Result.BUSY)
		{
			switch (context.setAwaitedLock(physicalLock))
			{
				case OK:
					switch (physicalLock.readLock(RequestMode.WAIT))
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
		context.addReadOnlyLock(physicalLock);
	}

	@Override
	@InvocationConstraint(types = ValidateWrite.ValidationAgent.class)
	public void validateWrite(Object writer, Object fieldOwner, String fieldName)
	{
		UserInterfaceActor actor;
		// TODO: not sure what to do when the owner is an actor and also a delegate (e.g. SelfPaintingListCell)
		if (fieldOwner instanceof UserInterfaceActorDelegate)
		{
			actor = ((UserInterfaceActorDelegate) fieldOwner).getActor();
		}
		else if (fieldOwner instanceof UserInterfaceActor)
		{
			actor = (UserInterfaceActor) fieldOwner;
		}
		else
		{
			Log.out(Tag.LOCK_WARNING, "Warining: attempt to validate write with an entity that cannot be correlated to a responsible actor: %s", fieldOwner
					.getClass().getName());
			return;
		}

		LockTransactionContext context = LockTransactionContext.get();
		context.checkInterrupted();
		if (context.actorsInstantiatedThisTransaction.contains(actor))
		{
			return;
		}

		synchronized (LockRegistry.adminLock)
		{
			UserInterfaceLock lock = LockRegistry.getInstance().locksByActor.get(actor);
			if (lock == null)
			{
				Log.out(Tag.LOCK_WARNING, "Warining: attempt to validate write with an actor that has no registered lock: %s");
				return;
			}

			if (!lock.getAutonomousLock().hasWritePermission())
			{
				Log.out(Tag.LOCK_WARNING, "Warning: attempt to write %s.%s without permission from the actor lock.", fieldOwner.getClass().getSimpleName(),
						fieldName);
			}
		}
	}
}
