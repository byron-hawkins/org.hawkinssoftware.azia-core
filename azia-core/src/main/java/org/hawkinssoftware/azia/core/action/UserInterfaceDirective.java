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

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@DomainRole.Join(membership = TransactionElement.class)
public abstract class UserInterfaceDirective implements UserInterfaceActorDelegate
{
	
	/**
	 * DOC comment task awaits.
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

	// TODO: kind of nebulous for the client to figure out this needs to be overridden for a meaningful notification
	// to be sent
	@InvocationConstraint(domains = { TransactionFacilitation.class, TransactionElement.class }, inherit = true)
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
