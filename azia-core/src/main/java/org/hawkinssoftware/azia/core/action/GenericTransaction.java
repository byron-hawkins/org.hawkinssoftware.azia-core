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

/**
 * A simple transaction for use in UI operations that have no special transaction requirements or behaviors. All
 * UIDirectives are posted back to the session for collaboration. All UIDirectives are committed in the order they were
 * added to the transaction, interleaving owner directives with contributed directives. It is not recommended that any
 * sub-transactions be spawned from a GenericTransaction, since nothing can be known about the purpose of a
 * GenericTransaction.
 * 
 * @author Byron Hawkins
 */
public class GenericTransaction implements UserInterfaceTransaction
{
	private final List<UserInterfaceDirective> transaction = new ArrayList<UserInterfaceDirective>();
	private Session session;

	@Override
	public void setSession(Session session)
	{
		this.session = session;
	}

	public void addAction(UserInterfaceDirective action)
	{
		transaction.add(action);
		session.postAction(action);
	}

	@Override
	public void transactionIntroduced(Class<? extends UserInterfaceTransaction> introducedTransactionType)
	{
	}

	@Override
	public void addActionsOn(List<UserInterfaceDirective> actions, UserInterfaceActor actor)
	{
		for (int i = transaction.size()-1; i >= 0; i--)
		{
			UserInterfaceDirective action = transaction.get(i);
			if (action.getActor() == actor)
			{
				actions.add(action);
			}
		}
	}

	@Override
	public void postDirectResponse(UserInterfaceDirective... actions)
	{
		for (UserInterfaceDirective action : actions)
		{
			transaction.add(action);
			session.postAction(action);
		}
	}

	@Override
	public void postDirectResponse(UserInterfaceNotification... notifications)
	{
		for (UserInterfaceNotification notification : notifications)
		{
			session.postNotification(notification);
		}
	}

	@Override
	public void postNotificationFromAnotherTransaction(UserInterfaceNotification notification)
	{
	}

	@Override
	public void commitTransaction()
	{
		for (UserInterfaceDirective action : transaction)
		{
			action.commit();
		}
	}

	@Override
	public void transactionRolledBack()
	{
	}

	@Override
	public boolean isEmpty()
	{
		return transaction.isEmpty();
	}
}
