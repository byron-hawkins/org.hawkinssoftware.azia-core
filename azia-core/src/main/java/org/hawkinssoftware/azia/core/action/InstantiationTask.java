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
package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceActor.SynchronizationRole;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.ConcurrentAccessException;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.lock.LockRegistry;
import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public interface InstantiationTask
{
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static abstract class Task
	{
		private final SynchronizationRole role;
		private final String description;

		public Task(SynchronizationRole role, String description)
		{
			this.role = role;
			this.description = description;
		}

		protected abstract void execute();

		public void start()
		{
			LockRegistry.getInstance().beginInstantiation(role, description);
			try
			{
				execute();
			}
			finally
			{
				LockRegistry.getInstance().endInstantiation();
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @param <T>
	 *            the generic type
	 * @author Byron Hawkins
	 */
	public static abstract class Producer<T extends Producer<T>>
	{
		private final SynchronizationRole role;
		private final String description;

		public Producer(SynchronizationRole role, String description)
		{
			this.role = role;
			this.description = description;
		}

		protected abstract void execute();

		@SuppressWarnings("unchecked")
		public T start()
		{
			LockRegistry.getInstance().beginInstantiation(role, description);
			try
			{
				execute();
			}
			finally
			{
				LockRegistry.getInstance().endInstantiation();
			}

			return (T) this;
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = TransactionParticipant.class)
	public static abstract class StandaloneInstantiationTask
	{
		private final SynchronizationRole role;
		private final String description;

		public StandaloneInstantiationTask(SynchronizationRole role, String description)
		{
			this.role = role;
			this.description = description;
		}

		protected abstract void executeInTransaction();

		public void start()
		{
			try
			{
				TransactionRegistry.executeTask(new UserInterfaceTask() {
					@Override
					protected boolean execute()
					{
						LockRegistry.getInstance().beginInstantiation(role, description);
						try
						{
							getTransaction(GenericTransaction.class);
							executeInTransaction();
						}
						finally
						{
							LockRegistry.getInstance().endInstantiation();
						}
						return true;
					}
				});
			}
			catch (ConcurrentAccessException e)
			{
				Log.out(Tag.CRITICAL, e, "Failed to execute actor producer");
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = TransactionElement.class)
	public static abstract class SubordinateTask
	{
		private final UserInterfaceActor actor;

		public SubordinateTask(UserInterfaceActorDelegate actor)
		{
			this.actor = actor.getActor();
		}

		protected abstract void execute();

		public void start()
		{
			LockRegistry.getInstance().beginSubordinateInstantiation(actor);
			try
			{
				execute();
			}
			finally
			{
				LockRegistry.getInstance().endInstantiation();
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @param <T>
	 *            the generic type
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = TransactionElement.class)
	public static abstract class SubordinateProducer<T extends SubordinateProducer<T>>
	{
		private final UserInterfaceActor actor;

		public SubordinateProducer(UserInterfaceActorDelegate actor)
		{
			this.actor = actor.getActor();
		}

		protected abstract void execute();

		@SuppressWarnings("unchecked")
		public T start()
		{
			LockRegistry.getInstance().beginSubordinateInstantiation(actor);
			try
			{
				execute();
			}
			finally
			{
				LockRegistry.getInstance().endInstantiation();
			}

			return (T) this;
		}
	}
}
