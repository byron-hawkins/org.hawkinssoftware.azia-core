package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.rns.core.role.DomainRole;

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
	public boolean isEmpty()
	{
		return true;
	}
}
