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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkinssoftware.azia.core.action.UserInterfaceTask.ConcurrentAccessException;
import org.hawkinssoftware.azia.core.action.UserInterfaceTask.RetryException;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransaction.ActorBasedContributor;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransaction.ActorBasedContributor.PendingTransaction;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.lock.LockRegistry;
import org.hawkinssoftware.rns.core.role.DomainRole;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Internal facilitation of the <code>UserInterfaceTransaction.Session</code>.
 * 
 * @author Byron Hawkins
 */
@DomainRole.Join(membership = TransactionFacilitation.class)
class UserInterfaceTransactionSession
{
	/**
	 * Internal implementation of the <code>UserInterfaceTransaction.Session</code>.
	 * 
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = TransactionFacilitation.class)
	private class TransactionSession implements UserInterfaceTransaction.Session
	{

		/**
		 * Pends a <code>UserInterfaceNotification</code> in association with a <code>UserInterfaceActorDelegate</code>
		 * if applicable, guaranteeing to lock that UIActor before sending the UINotification when a UIActor is
		 * available.
		 * 
		 * @author Byron Hawkins
		 * @see UserInterfaceActorDelegate
		 * @see UserInterfaceNotification
		 */
		@DomainRole.Join(membership = TransactionFacilitation.class)
		private class PendingBroadcast
		{
			private final UserInterfaceActorDelegate actor;
			private final UserInterfaceNotification notification;

			public PendingBroadcast(UserInterfaceNotification notification)
			{
				this(null, notification);
			}

			public PendingBroadcast(UserInterfaceActorDelegate actor, UserInterfaceNotification notification)
			{
				this.actor = actor;
				this.notification = notification;
			}

			void send()
			{
				if (actor == null)
				{
					broadcastNotification(transaction, notification);
				}
				else
				{
					LockRegistry.getInstance().lockForAssembly(transaction, actor.getActor());
					broadcastAction(actor.getActor(), transaction, notification);
				}
			}
		}

		private final UserInterfaceTransaction transaction;

		private final Set<Class<? extends UserInterfaceNotification>> requestedNotificationTypes = new HashSet<Class<? extends UserInterfaceNotification>>();
		private final Set<Class<? extends UserInterfaceTransaction>> requestedNotificationSources = new HashSet<Class<? extends UserInterfaceTransaction>>();
		private final Multimap<Class<? extends UserInterfaceTransaction>, Class<? extends UserInterfaceNotification>> requestedNotificationTypesBySource = ArrayListMultimap
				.create();
		private final Set<UserInterfaceNotification> postedNotifications = new HashSet<UserInterfaceNotification>();

		private List<PendingBroadcast> pendingBroadcast = new ArrayList<PendingBroadcast>();

		TransactionSession(UserInterfaceTransaction transaction)
		{
			this.transaction = transaction;
			transaction.setSession(this);
		}

		@Override
		public void requestNotificationByType(Class<? extends UserInterfaceNotification> notificationType)
		{
			requestedNotificationTypes.add(notificationType);

			for (UserInterfaceNotification notification : notificationsByNotificationType.get(notificationType))
			{
				if (!postedNotifications.contains(notification))
				{
					postNotificationToClient(notification);
				}
			}
		}

		@Override
		public void requestNotificationByTransactionType(Class<? extends UserInterfaceTransaction> transactionType)
		{
			requestedNotificationSources.add(transactionType);

			for (UserInterfaceNotification notification : notificationsByTransactionType.get(transactionType))
			{
				if (!postedNotifications.contains(notification))
				{
					postNotificationToClient(notification);
				}
			}
		}

		@Override
		public void requestSpecificNotification(Class<? extends UserInterfaceTransaction> transactionType,
				Class<? extends UserInterfaceNotification> notificationType)
		{
			requestedNotificationTypesBySource.put(transactionType, notificationType);

			for (UserInterfaceNotification notification : notificationsByTransactionType.get(transactionType))
			{
				if ((notification.getClass() == notificationType) && !postedNotifications.contains(notification))
				{
					postNotificationToClient(notification);
				}
			}
		}

		/**
		 * @JTourBusStop 3.2, Virtual encapsulation in an Azia user interface transaction, MouseEventTransaction
		 *               initiated:
		 * 
		 *               The notification is broadcast by the Azia transaction engine to the topTile (the "actor").
		 */
		@Override
		public void postAction(UserInterfaceActorDelegate actor, UserInterfaceNotification notification)
		{
			pendingBroadcast.add(new PendingBroadcast(actor, notification));
		}

		@Override
		public void postAction(UserInterfaceDirective action)
		{
			postAction(action, action.createNotification());
		}

		@Override
		public void postNotification(UserInterfaceNotification notification)
		{
			pendingBroadcast.add(new PendingBroadcast(notification));
		}

		void broadcast()
		{
			List<PendingBroadcast> dispatch = new ArrayList<PendingBroadcast>(pendingBroadcast);
			pendingBroadcast.clear();
			for (PendingBroadcast broadcast : dispatch)
			{
				broadcast.send();
			}
		}

		void actionPostedToSession(UserInterfaceTransaction source, UserInterfaceNotification notification)
		{
			if (source == this)
			{
				return;
			}

			boolean requested = requestedNotificationSources.contains(source.getClass());
			requested |= requestedNotificationTypes.contains(notification.getClass());
			requested |= requestedNotificationTypesBySource.get(source.getClass()).contains(notification.getClass());

			if (requested)
			{
				postNotificationToClient(notification);
			}
		}

		private void postNotificationToClient(UserInterfaceNotification notification)
		{
			transaction.postNotificationFromAnotherTransaction(notification);
			postedNotifications.add(notification);
		}
	}

	/**
	 * Defines the sequence of phases of a <code>UserInterfaceTransaction</code>.
	 * 
	 * @author Byron Hawkins
	 */
	private enum Phase
	{
		/**
		 * The session is idle, there is no current transaction.
		 */
		IDLE,
		/**
		 * The current transaction is being assembled. Each instance of <code>UserInterfaceActor</code> related to the
		 * <code>UserInterfaceDirective</code>s in the transaction are locked in assembly phase, meaning fields
		 * protected under their locks can be read, and cannot be allocated to any other assembly transaction until this
		 * one has committed or rolled back.
		 */
		ASSEMBLY,
		/**
		 * The current transaction is being committed. Fields protected under the locks of the
		 * <code>UserInterfaceActor</code>s in this transaction are writable.
		 */
		COMMIT,
		/**
		 * The current transaction is being post-processed. All <code>PostProcessor</code>s are executed at this time.
		 * All locks have been released.
		 */
		POST_PROCESSING;
	}

	private Phase phase = Phase.IDLE;
	private final Map<Class<? extends UserInterfaceTransaction>, TransactionSession> sessions = new HashMap<Class<? extends UserInterfaceTransaction>, TransactionSession>();

	// these are notification listeners
	private final Multimap<Class<? extends UserInterfaceTransaction>, UserInterfaceNotification> notificationsByTransactionType = ArrayListMultimap.create();
	private final Multimap<Class<? extends UserInterfaceNotification>, UserInterfaceNotification> notificationsByNotificationType = ArrayListMultimap.create();

	private final List<UserInterfaceTask> taskStack = new ArrayList<UserInterfaceTask>();

	List<UserInterfaceDirective> getActionsOn(UserInterfaceActor actor)
	{
		List<UserInterfaceDirective> actions = new ArrayList<UserInterfaceDirective>();
		for (TransactionSession session : sessions.values())
		{
			session.transaction.addActionsOn(actions, actor);
		}
		return actions;
	}

	void executeTask(UserInterfaceTask task) throws UserInterfaceTask.ConcurrentAccessException
	{
		switch (phase)
		{
			case COMMIT:
				throw new IllegalStateException("Attempt to execute a task in phase " + phase);
			case POST_PROCESSING:
				if (task.type != UserInterfaceTask.Type.POST_PROCESSING)
				{
					throw new IllegalStateException("Attempt to execute a " + task.type + " task in phase " + phase);
				}
				break;
			default:
				if (task.type != UserInterfaceTask.Type.PROCESSING)
				{
					throw new IllegalStateException("Attempt to execute a " + task.type + " task in phase " + phase);
				}
				break;
		}

		task.setSession(this);

		taskStack.add(task);
		if (taskStack.size() == 1)
		{
			try
			{
				executeOutermostTask(task);
			}
			finally
			{
				taskStack.remove(taskStack.size() - 1);
			}
		}
	}

	private boolean executeAndBroadcast(UserInterfaceTask task)
	{
		try
		{
			if (task.execute())
			{
				boolean complete;
				do
				{
					for (TransactionSession session : sessions.values())
					{
						if (session.transaction instanceof UserInterfaceTransaction.Iterative)
						{
							UserInterfaceTransaction.Iterative iterative = (UserInterfaceTransaction.Iterative) session.transaction;
							while (iterative.hasMoreIterations())
							{
								session.broadcast();
								iterative.iterate();
							}
						}
						session.broadcast();
					}

					complete = true;
					for (TransactionSession session : sessions.values())
					{
						if (!session.pendingBroadcast.isEmpty())
						{
							complete = false;
							break;
						}

						if (session.transaction instanceof UserInterfaceTransaction.Iterative)
						{
							if (((UserInterfaceTransaction.Iterative) session.transaction).hasMoreIterations())
							{
								complete = false;
								break;
							}
						}
					}
				}
				while (!complete);
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			task.setSession(null);
		}
	}

	private void executeOutermostTask(UserInterfaceTask task) throws ConcurrentAccessException
	{
		int retryCount = 0;

		while (true)
		{
			boolean reachedCommit = false;
			try
			{
				boolean successfulExecution = false;
				try
				{
					successfulExecution = executeAndBroadcast(task);

					while (taskStack.size() > 1)
					{
						// the sequence could be made to better match the task submission if necessary
						UserInterfaceTask subtask = taskStack.remove(taskStack.size() - 1);
						executeAndBroadcast(subtask);
					}
				}
				finally
				{
					if (phase == Phase.ASSEMBLY)
					{
						reachedCommit = true;
						if (successfulExecution)
						{
							/**
							 * @JTourBusStop 4.1, ReCopyHandler participates in mouse and keyboard transactions,
							 *               Internal transaction engine commits:
							 * 
							 *               After executing all tasks in the current transaction (above), this
							 *               transaction is committed (4.11), resulting in the commit of each of its
							 *               actions (4.12).
							 */
							commitSession();
						}
						else
						{
							rollbackSession();
						}
					}
				}
				break;
			}
			catch (RetryException e)
			{
				if (retryCount >= task.getRetryCount())
				{
					throw new ConcurrentAccessException("Failed to acquire all necessary locks for this task after " + task.getRetryCount() + " retries.", e);
				}
			}
			System.out.println("Retry (reach commit: " + reachedCommit + ")");
			retryCount++;
			task.setSession(this);
		}
	}

	void failCurrentTask(UserInterfaceTask.CollisionStatus status)
	{
		if (phase == Phase.IDLE)
		{
			throw new IllegalStateException("Attempt to fail a task when no task is in progress (phase is " + phase + ")");
		}

		LockRegistry.getInstance().emergencyReleaseContext();
		terminateSession();
		taskStack.get(taskStack.size() - 1).returnAndRetry(status);
	}

	private UserInterfaceTransaction beginSession(Class<? extends UserInterfaceTransaction> transactionType)
	{
		if (phase != Phase.IDLE)
		{
			throw new IllegalStateException("Attempt to begin a session on a thread having a session in " + phase + " phase.");
		}

		for (UserInterfaceTransaction.PostProcessor postProcessor : TransactionRegistryCoordinator.getInstance().getPostProcessors())
		{
			postProcessor.sessionStarting();
		}
		LockRegistry.getInstance().beginSession();

		phase = Phase.ASSEMBLY;
		return joinSession(transactionType);
	}

	UserInterfaceTransaction joinSession(Class<? extends UserInterfaceTransaction> transactionType)
	{
		switch (phase)
		{
			case IDLE:
				return beginSession(transactionType);
			case COMMIT:
				throw new IllegalStateException("Attempt to join a session on a thread having a session in " + phase + " phase.");
		}

		TransactionSession session = sessions.get(transactionType);
		if (session == null)
		{
			UserInterfaceTransaction transaction = null;
			try
			{
				transaction = transactionType.newInstance();
			}
			catch (Throwable t)
			{
				throw new RuntimeException(
						"Failed to instantiate a transaction. All transactions must have default, no-arg constructors. This constitutes total application failure.",
						t);
			}

			session = new TransactionSession(transaction);
			LockRegistry.getInstance().beginTransaction(transaction);

			for (TransactionSession existingSession : sessions.values())
			{
				existingSession.transaction.transactionIntroduced(transactionType);
				transaction.transactionIntroduced(existingSession.transaction.getClass());
			}

			sessions.put(transactionType, session);

			for (TransactionRegistry.Listener listener : TransactionRegistryCoordinator.getInstance().getListeners())
			{
				Class<? extends UserInterfaceTransaction> joinType = listener.transactionInitiated(transactionType);
				if (joinType != null)
				{
					joinSession(joinType);
				}
			}
		}
		return session.transaction;
	}

	/**
	 * @JTourBusStop 4.11, ReCopyHandler participates in mouse and keyboard transactions, Internal transaction commit
	 *               process:
	 * 
	 *               Transaction listeners may spontaneously attach a new transaction to any running transaction, and
	 *               each transaction in the group is represented here as a TransactionSession. The commit is applied to
	 *               each of these sessions in sequence.
	 */
	private void commitSession()
	{
		if (phase != Phase.ASSEMBLY)
		{
			throw new IllegalStateException("Attempt to commit a session on a thread having a session in " + phase + " phase.");
		}
		phase = Phase.COMMIT;

		// commit the regular transactions
		LockRegistry.getInstance().commitLock();
		for (TransactionSession session : sessions.values())
		{
			if (session.transaction.isEmpty())
			{
				continue;
			}

			session.transaction.commitTransaction();
		}
		LockRegistry.getInstance().commitUnlock();

		sessions.clear();
		postProcessSession();
	}

	private void postProcessSession()
	{
		if (phase != Phase.COMMIT)
		{
			throw new IllegalStateException("Attempt to post-process a session on a thread having a session in " + phase + " phase.");
		}
		phase = Phase.POST_PROCESSING;

		for (UserInterfaceTransaction.PostProcessor postProcessor : TransactionRegistryCoordinator.getInstance().getPostProcessors())
		{
			postProcessor.sessionCommitted();
		}

		while (taskStack.size() > 1)
		{
			// the sequence could be made to better match the task submission if necessary
			UserInterfaceTask subtask = taskStack.remove(taskStack.size() - 1);
			executeAndBroadcast(subtask);
		}

		// commit the post-processing transactions
		LockRegistry.getInstance().commitLock();
		for (TransactionSession session : sessions.values())
		{
			if (session.transaction.isEmpty())
			{
				continue;
			}

			session.transaction.commitTransaction();
		}
		LockRegistry.getInstance().commitUnlock();

		for (UserInterfaceTransaction.PostProcessor postProcessor : TransactionRegistryCoordinator.getInstance().getPostProcessors())
		{
			postProcessor.postProcessingCommitted();
		}

		terminateSession();
	}

	void rollbackSession()
	{
		for (TransactionSession session : sessions.values())
		{
			session.transaction.transactionRolledBack();
		}

		terminateSession();

		try
		{
			for (UserInterfaceTransaction.PostProcessor postProcessor : TransactionRegistryCoordinator.getInstance().getPostProcessors())
			{
				postProcessor.transactionRolledBack();
			}
		}
		catch (Throwable t)
		{
			// no margin for error here--convenience is not important
			t.printStackTrace();
		}
	}

	private void terminateSession()
	{
		notificationsByNotificationType.clear();
		notificationsByTransactionType.clear();
		// TODO: confusing to call this "sessions" when it refers to the transactions within a single UITxnSession: one
		// is session per thread, the other is session per txn
		sessions.clear();
		phase = Phase.IDLE;
	}

	void broadcastAction(UserInterfaceActor actor, UserInterfaceTransaction source, UserInterfaceNotification notification)
	{
		notificationsByTransactionType.put(source.getClass(), notification);
		notificationsByNotificationType.put(notification.getClass(), notification);

		for (TransactionSession session : sessions.values())
		{
			session.actionPostedToSession(source, notification);
		}

		PendingTransaction contributions = new PendingTransaction();
		for (ActorBasedContributor contributor : TransactionRegistry.getInstance().getActorBasedContributors(actor))
		{
			contributor.actionPosted(notification, contributions);
		}

		source.postDirectResponse(contributions.lockDirectivesForAssembly(source));
		source.postDirectResponse(contributions.getNotifications());
	}

	void broadcastNotification(UserInterfaceTransaction source, UserInterfaceNotification notification)
	{
		for (TransactionSession session : sessions.values())
		{
			session.actionPostedToSession(source, notification);
		}

		if (notification instanceof UserInterfaceNotification.Directed)
		{
			PendingTransaction contributions = new PendingTransaction();
			UserInterfaceActor actor = ((UserInterfaceNotification.Directed) notification).getActor();
			for (ActorBasedContributor contributor : TransactionRegistry.getInstance().getActorBasedContributors(actor))
			{
				contributor.actionPosted(notification, contributions);
			}
			source.postDirectResponse(contributions.lockDirectivesForAssembly(source));
			source.postDirectResponse(contributions.getNotifications());
		}
	}
}
