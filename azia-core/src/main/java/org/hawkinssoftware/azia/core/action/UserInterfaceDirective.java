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

import java.util.Collections;
import java.util.List;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * A UserInterfaceDirective constitutes a proposition to modify one or more fields that are locked under central
 * transaction management. The directive must be contributed to an active <code>UserInterfaceTransaction</code>, and
 * <code>commit()</code> will be called by the transaction engine. Contribution may occur at the beginning of a
 * transaction, as provided for by the particular subclass of UITransaction, or it may occur in response to a
 * <code>UserInterfaceNotification</code> received by a transaction participant.
 * <p>
 * Upon contribution to the transaction, all its participants will receive the notification produced by
 * <code>createNotification()</code>, so a UIDirective subclass wishing to be publicly known for collaboration purposes
 * must define and return a meaningful notification.
 * 
 * @author Byron Hawkins
 * @see UserInterfaceTransaction
 * @see UserInterfaceNotification
 */
@DomainRole.Join(membership = TransactionElement.class)
public abstract class UserInterfaceDirective implements UserInterfaceActorDelegate
{
	/**
	 * A generic notification to be sent on behalf of any <code>UserInterfaceDirective</code> subclass not wishing to
	 * broadcast a more specific notification about its participation in a transaction.
	 * 
	 * @author Byron Hawkins
	 */
	public class Notification extends UserInterfaceNotification
	{
		public String getId()
		{
			return id;
		}

		public Class<? extends UserInterfaceActor> getActorType()
		{
			return actor.getClass();
		}

		public Class<? extends UserInterfaceDirective> getDirectiveType()
		{
			return UserInterfaceDirective.this.getClass();
		}
	}

	public final String id;

	private final UserInterfaceActor actor;

	public UserInterfaceDirective(UserInterfaceActorDelegate actor)
	{
		id = getClass().getName();
		this.actor = actor.getActor();
	}

	public UserInterfaceDirective(String id, UserInterfaceActorDelegate actor)
	{
		this.id = id;
		this.actor = actor.getActor();
	}

	public final UserInterfaceActor getActor()
	{
		return actor;
	}

	public List<UserInterfaceActorPreview> getPreviews(UserInterfaceDirective action)
	{
		return Collections.emptyList();
	}

	// TODO: kind of nebulous for the client to figure out this needs to be overridden for a meaningful notification
	// to be sent
	@InvocationConstraint(domains = { TransactionFacilitation.class }, inherit = true)
	public UserInterfaceNotification createNotification()
	{
		return new Notification();
	}

	@InvocationConstraint(extendedTypes = UserInterfaceTransaction.class)
	public void commit()
	{
		actor.apply(this);
	}
}
