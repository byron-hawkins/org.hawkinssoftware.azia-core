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

import java.util.List;

import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@DomainRole.Join(membership = UserInterfaceTransactionDomains.TransactionElement.class)
public class ReadOnlyTransaction implements UserInterfaceTransaction
{
	private Session session;

	@Override
	public void setSession(Session session)
	{
		this.session = session;
	}

	public void addAction(UserInterfaceDirective action)
	{
		throw new UnsupportedOperationException("The read-only transaction does not support directives.");
	}

	@Override
	public void transactionIntroduced(Class<? extends UserInterfaceTransaction> introducedTransactionType)
	{
	}

	@Override
	public void addActionsOn(List<UserInterfaceDirective> actions, UserInterfaceActor actor)
	{
		// takes no action
	}

	@Override
	public void postDirectResponse(UserInterfaceDirective... actions)
	{
		throw new UnsupportedOperationException("The read-only transaction does not support directives.");
	}

	@Override
	public void postDirectResponse(UserInterfaceNotification... notifications)
	{
		throw new UnsupportedOperationException("The read-only transaction does not support notifications.");
	}

	@Override
	public void postNotificationFromAnotherTransaction(UserInterfaceNotification notification)
	{
	}

	@Override
	public void commitTransaction()
	{
	}

	@Override
	public void transactionRolledBack()
	{
	}

	@Override
	public boolean isEmpty()
	{
		return true;
	}
}
