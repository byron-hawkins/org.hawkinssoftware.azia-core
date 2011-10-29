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
import java.util.List;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.lock.LockRegistry;
import org.hawkinssoftware.rns.core.publication.ExtensionConstraint;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.publication.VisibilityConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.util.DefinesIdentity;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@InvocationConstraint(domains = TransactionFacilitation.class)
@VisibilityConstraint(domains = { TransactionParticipant.class, TransactionFacilitation.class, TransactionElement.class }, inherit = true)
@ExtensionConstraint(domains = TransactionElement.class)
@DefinesIdentity
@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
public interface UserInterfaceTransaction
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@InvocationConstraint(domains = TransactionFacilitation.class)
	@ExtensionConstraint(domains = TransactionParticipant.class)
	@DomainRole.Join(membership = TransactionParticipant.class)
	@DefinesIdentity
	public interface ActorBasedContributor
	{
		
		/**
		 * DOC comment task awaits.
		 * 
		 * @author Byron Hawkins
		 */
		@DomainRole.Join(membership = TransactionElement.class)
		@InvocationConstraint(domains = { TransactionParticipant.class, TransactionFacilitation.class })
		@ExtensionConstraint(domains = TransactionFacilitation.class)
		public static class PendingTransaction
		{
			private final List<UserInterfaceDirective> contributions = new ArrayList<UserInterfaceDirective>();
			private final List<UserInterfaceNotification> notifications = new ArrayList<UserInterfaceNotification>();
  
			public void contribute(UserInterfaceDirective action)
			{
				contributions.add(action);
			}

			public void contribute(UserInterfaceNotification notification)
			{
				notifications.add(notification);
			}

			UserInterfaceDirective[] lockDirectivesForAssembly(UserInterfaceTransaction transaction)
			{
				UserInterfaceDirective[] lockedContributions = new UserInterfaceDirective[contributions.size()];
				for (int i = 0; i < lockedContributions.length; i++)
				{
					lockedContributions[i] = contributions.get(i);
					LockRegistry.getInstance().lockForAssembly(transaction, lockedContributions[i].getActor());
				}
				contributions.clear();
				return lockedContributions;
			}

			UserInterfaceNotification[] getNotifications()
			{
				return notifications.toArray(new UserInterfaceNotification[0]);
			}
		}

		// TODO: will the contributor need any info about the transaction (or sub)?
		void actionPosted(UserInterfaceNotification notification, PendingTransaction transaction);
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@VisibilityConstraint(domains = { TransactionElement.class, TransactionFacilitation.class })
	@InvocationConstraint(domains = TransactionElement.class)
	@ExtensionConstraint(domains = TransactionFacilitation.class)
	public interface Session
	{
		void requestNotificationByType(Class<? extends UserInterfaceNotification> notificationType);

		void requestNotificationByTransactionType(Class<? extends UserInterfaceTransaction> transactionType);

		void requestSpecificNotification(Class<? extends UserInterfaceTransaction> transactionType, Class<? extends UserInterfaceNotification> notificationType);

		void postAction(UserInterfaceActorDelegate actor, UserInterfaceNotification notification);

		void postAction(UserInterfaceDirective action);

		// TODO: seems risky to leave these in the hands of the transaction--could forward them by default, and allow
		// txn to override. But txn is supposed to be a highly responsible entity, so maybe it's ok in this case.
		void postNotification(UserInterfaceNotification notification);
	}

	// TODO: domains should somehow prevent this from getting tangled up in the transactions and directives
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@InvocationConstraint(domains = TransactionFacilitation.class)
	@ExtensionConstraint(domains = TransactionElement.class)
	@DomainRole.Join(membership = TransactionElement.class)
	public interface PostProcessor
	{
		void sessionStarting();

		void sessionCommitted();

		void postProcessingCommitted();

		void transactionRolledBack();
	}
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public interface Iterative extends UserInterfaceTransaction
	{
		boolean hasMoreIterations();

		void iterate();
	}

	void setSession(Session session);

	void transactionIntroduced(Class<? extends UserInterfaceTransaction> introducedTransactionType);

	void postDirectResponse(UserInterfaceDirective... actions);

	void postDirectResponse(UserInterfaceNotification... notifications);

	void postNotificationFromAnotherTransaction(UserInterfaceNotification notification);

	void commitTransaction();
	
	@InvocationConstraint(domains = { TransactionFacilitation.class, TransactionElement.class, TransactionParticipant.class })
	boolean isEmpty();
}
