package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceActor.SynchronizationRole;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.ConcurrentAccessException;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.lock.LockRegistry;
import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.role.DomainRole;

public interface InstantiationTask
{
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

	public static abstract class SubordinateTask
	{
		private final UserInterfaceActor actor;

		public SubordinateTask(UserInterfaceActor actor)
		{
			this.actor = actor;
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

	public static abstract class SubordinateProducer<T extends SubordinateProducer<T>>
	{
		private final UserInterfaceActor actor;

		public SubordinateProducer(UserInterfaceActor actor)
		{
			this.actor = actor;
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
