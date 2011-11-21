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

import java.util.Map;
import java.util.Set;

import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.ThreadStateValidation;
import org.hawkinssoftware.rns.core.collection.CollectionAccessValidator;
import org.hawkinssoftware.rns.core.lock.SemaphoreHook;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.role.TypeRole;

/**
 * Validates access to the fields of the LockRegistry and supporting classes. It does not validate anything outside this
 * one specific package.
 * 
 * @author b
 */
@ExecutionPath.NoFrame
@DomainRole.Join(membership = LockManagement.class)
class LockAccessValidator implements SemaphoreHook
{
	// enabled flag is recognized within the collection
	/**
	 * DOC comment task awaits.
	 * 
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	static class LockRegistryMapValidator<K, V> implements CollectionAccessValidator<Map<K, V>>
	{
		private final String fieldName;

		LockRegistryMapValidator(String fieldName)
		{
			this.fieldName = fieldName;
		}

		@Override
		public void validateRead(Map<K, V> collection, String methodName, Object... args)
		{
			if (getInstance().heldSemaphore != Semaphore.LOCK_REGISTRY_ADMIN)
			{
				throw new IllegalStateException("Attempt to " + fieldName + "." + methodName + "() without holding the lock.");
			}
		}

		@Override
		public void validateWrite(Map<K, V> collection, String methodName, Object... args)
		{
			if (getInstance().heldSemaphore != Semaphore.LOCK_REGISTRY_ADMIN)
			{
				throw new IllegalStateException("Attempt to " + fieldName + "." + methodName + "() without holding the lock.");
			}
		}
	}

	// enabled flag is recognized within the collection
	/**
	 * DOC comment task awaits.
	 * 
	 * @param <T>
	 *            the generic type
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	static class LockTransactionContextSetValidator<T> implements CollectionAccessValidator<Set<T>>
	{
		private final String fieldName;

		LockTransactionContextSetValidator(String fieldName)
		{
			this.fieldName = fieldName;
		}

		@Override
		public void validateRead(Set<T> collection, String methodName, Object... args)
		{
			TypeRole role = ExecutionPath.getReceiverRole();
			if (role.hasRole(ThreadStateValidation.INSTANCE) && (getInstance().heldSemaphore != Semaphore.LOCK_TRANSACTION_CONTEXT))
			{
				throw new IllegalStateException("Attempt to " + fieldName + "." + methodName + "() from " + role + " without holding the lock.");
			}
		}

		@Override
		public void validateWrite(Set<T> collection, String methodName, Object... args)
		{
			if (getInstance().heldSemaphore != Semaphore.LOCK_TRANSACTION_CONTEXT)
			{
				throw new IllegalStateException("Attempt to " + fieldName + "." + methodName + "() without holding the lock.");
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	static class FieldAccessValidator
	{
		static void validateLockTransactionContextFieldRead(Object writer, Object fieldOwner, String fieldName)
		{
			if (!enabled)
				return;

			if (getInstance().heldSemaphore != Semaphore.LOCK_TRANSACTION_CONTEXT)
			{
				throw new IllegalStateException("Attempt to read " + fieldOwner.getClass().getSimpleName() + "." + fieldName + " without holding the lock.");
			}
		}

		public static void validateLockTransactionContextFieldWrite(Object writer, Object fieldOwner, String fieldName)
		{
			if (!enabled)
				return;

			if (getInstance().heldSemaphore != Semaphore.LOCK_TRANSACTION_CONTEXT)
			{
				throw new IllegalStateException("Attempt to write " + fieldOwner.getClass().getSimpleName() + "." + fieldName + " without holding the lock.");
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	enum Semaphore
	{
		LOCK_REGISTRY_ADMIN(LockRegistry.adminLock),
		LOCK_TRANSACTION_CONTEXT(LockTransactionContext.class);

		final Object semaphore;

		private Semaphore(Object semaphore)
		{
			this.semaphore = semaphore;
		}

		static Semaphore forObject(Object o)
		{
			for (Semaphore semaphore : Semaphore.values())
			{
				if (semaphore.semaphore == o)
				{
					return semaphore;
				}
			}
			throw new IllegalArgumentException("The semaphore for object " + o + " (a " + o.getClass().getName() + ") is not known to the "
					+ LockAccessValidator.class.getSimpleName());
		}
	}

	public static LockAccessValidator getInstance()
	{
		return LockTransactionContext.get().semaphoreAccessValidator;
	}

	private static final boolean enabled = System.getProperty("disable-access-validation") == null;

	private final LockTransactionContext context;

	private Semaphore heldSemaphore = null;
	private int semaphoreHoldCount = 0;

	LockAccessValidator(LockTransactionContext context)
	{
		this.context = context;
	}

	@Override
	public void attemptingAcquisition(Object semaphore)
	{
		if (!enabled)
			return;

		Semaphore attempt = Semaphore.forObject(semaphore);

		if ((heldSemaphore != null) && (heldSemaphore != attempt))
		{
			throw new IllegalStateException("Attempt to acquire the " + attempt + " lock while the " + heldSemaphore + " lock is held.");
		}
	}

	@Override
	public void semaphoreAcquired(Object semaphore)
	{
		if (!enabled)
			return;

		if (heldSemaphore == null)
		{
			heldSemaphore = Semaphore.forObject(semaphore);
			UserInterfaceLockDomains.LockManagement.ContainmentConstraint.INSTANCE.semaphoreAcquired(heldSemaphore);
		}
		else
		{
			semaphoreHoldCount++;
		}
	}

	@Override
	public void semaphoreReleased(Object semaphore)
	{
		if (!enabled)
			return;

		if (semaphoreHoldCount > 0)
		{
			semaphoreHoldCount--;
		}
		else
		{
			heldSemaphore = null;

			UserInterfaceLockDomains.LockManagement.ContainmentConstraint.INSTANCE.semaphoreReleased();
		}
	}

	void attemptingAcquisition(AutonomousLock lock, AutonomousLock.Level level)
	{
		if (!enabled)
			return;

		if (heldSemaphore != null)
		{
			throw new IllegalStateException("Attempt to acquire an actor lock while holding the " + heldSemaphore + " lock.");
		}
	}

	void lockAcquired(AutonomousLock lock, AutonomousLock.Level level)
	{
	}

	void lockReleased(AutonomousLock lock, AutonomousLock.Level level)
	{
	}
}
