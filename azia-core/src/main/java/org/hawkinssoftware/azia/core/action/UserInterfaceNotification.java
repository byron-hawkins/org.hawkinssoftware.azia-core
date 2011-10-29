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
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@DomainRole.Join(membership = TransactionElement.class)
public abstract class UserInterfaceNotification
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = TransactionElement.class)
	public static abstract class Directed extends UserInterfaceNotification implements UserInterfaceActorDelegate
	{
		private final UserInterfaceActor actor;

		public Directed(UserInterfaceActorDelegate actor)
		{
			this.actor = actor.getActor();
		}

		@Override
		public UserInterfaceActor getActor()
		{
			return actor;
		}
	}
}
