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
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.rns.core.publication.ExtensionConstraint;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * By implementing this interface, the Delegate declares a concrete association with exactly one instance of
 * <code>UserInterfaceActor</code>, which it specifies by the return value of <code>getActor()</code>. It is expected
 * that this actor will never change, and if it does, actor-based features may not work correctly.
 * 
 * @author Byron Hawkins
 * @see UserInterfaceActor
 */
@InvocationConstraint(domains = { TransactionFacilitation.class, TransactionElement.class })
@ExtensionConstraint(domains = TransactionParticipant.class)
@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
public interface UserInterfaceActorDelegate
{
	UserInterfaceActor getActor();

	public static final class Comparator
	{
		public static boolean isSameActor(UserInterfaceActorDelegate first, UserInterfaceActorDelegate second)
		{
			return first.getActor() == second.getActor();
		}
	}
}
