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
 * An aggregation of <code>UserInterfaceDirective</code>s which must be committed under special conditions:
 * <ul>
 * <li>All data to be modified by each directive must be locked. This is most often done automatically using the
 * <code>@ValidateWrite</code> annotation.</li>
 * <li>All directives must be committed together, or the transaction must be rolled back.</li>
 * </ul>
 * A transaction can only be initiated by submitting a subclass of <code>UserInterfaceTask</code> to
 * <code>TransactionRegistry.execute()</code>.
 * <p>
 * Collaboration is automatically supported by all UITransactions. When execution begins, the transaction manager will
 * assign a session via <code>setSession()</code>. For any UIDirective added to a UITransaction, the transaction may
 * post it to the session using <code>session.postAction()</code>, which causes all instances of
 * <code>ActorBasedContributor</code> associated with the <code>UIDirective.getActor()</code> to receive an
 * <code>ActorBasedContributor.actionPosted()</code> call with the <code>UserInterfaceNotification</code> corresponding
 * to that directive. Each participant may then contribute UIDirectives to the <code>PendingTransaction</code> (which is
 * a parameter of that <code>actionPosted()</code> method).
 * <p>
 * Client code will most commonly implement collaboration by installing a <code>UserInterfaceHandler</code> into a
 * system-supplied base implementation of <code>UserInterfaceActor</code>, such as a <code>VirtualComponent</code> or
 * <code>AbstractEventDispatch</code>. All UIDirectives and UINotifications posted to that actor will be available to
 * the UIHandler according to its action and notification routing rules.
 * 
 * @author Byron Hawkins
 * @see UserInterfaceActor
 * @see UserInterfaceDirective
 * @see UserInterfaceNotification
 * @see UserInterfaceHandler
 * @see UserInterfaceTransaction.PendingTransaction
 * @see UserInterfaceTransaction.ActorBasedContributor
 * @see TransactionRegistry
 */
@InvocationConstraint(domains = TransactionFacilitation.class)
@VisibilityConstraint(domains = { TransactionParticipant.class, TransactionFacilitation.class, TransactionElement.class }, inherit = true)
@ExtensionConstraint(domains = TransactionElement.class)
@DefinesIdentity
@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
public interface UserInterfaceTransaction
{
	/**
	 * Implemented by any entity wishing to receive <code>actionPosted()</code> notification when a
	 * <code>UserInterfaceDirective</code> is posted to a particular <code>UserInterfaceActor</code>. The implementor
	 * must register itself with that UIActor via
	 * <code>TransactionRegistry.getInstance().addActorBasedContributor()</code>. Currently it is discouraged for client
	 * code to use this kind of internal wiring, and instead clients are encouraged to register instances of
	 * <code>UserInterfaceHandler</code> with the system-supplied implementors of <code>UserInterfaceActor</code> such
	 * as <code>VirtualComponent</code>, <code>AbstractEventDispatch</code>, etc.
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
		 * Proxy container for collecting contributions to an active transaction. The PendingTransaction is made
		 * available as a system-supplied parameter of <code>ActorBasedContributor.actionPosted()</code>.
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

			@InvocationConstraint(types = UserInterfaceTransactionSession.class)
			PendingTransaction()
			{
			}

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

		void actionPosted(UserInterfaceNotification notification, PendingTransaction transaction);
	}

	/**
	 * The Session is only implemented internally, and is assigned to an instance of
	 * <code>UserInterfaceTransaction</code> when it is instantiated (during a <code>UserInterfaceTask</code>).
	 * 
	 * @author Byron Hawkins
	 * @see UserInterfaceTask
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
	 * By implementing this interface, the PostProcessor declares itself to require execution at the end of a
	 * transaction, after all <code>UserInterfaceDirective</code>s have been committed and their locks released.
	 * Typically, an PostProcessor will accumulate units of work during a transaction via static methods which delegate
	 * to an internal ThreadLocal. This allows the client code to invoke the PostProcessor in <i>ad hoc</i> manner. See
	 * the <code>RepaintRequestManager</code> for an example.
	 * 
	 * @author Byron Hawkins
	 * @see UserInterfaceDirective
	 * @see RepaintRequestManager
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
	 * A <code>UserInterfaceTransaction</code> optionally extends this specialized interface to indicate that it
	 * contains iterative structures in the notification process. For example, the <code>PaintTransaction</code> begins
	 * with only one <code>UserInterfaceDirective</code>, which submits a <code>PaintIncludeNotification</code> for each
	 * entity residing within its physical boundaries. The transaction manager uses this interface to coordinate the
	 * iteration of these inclusions.
	 * 
	 * @author Byron Hawkins
	 * @see UserInterfaceDirective
	 * @see PaintTransaction
	 * @see PaintIncludeNotification
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
