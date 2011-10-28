package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.rns.core.role.DomainRole;

public interface UserInterfaceTransactionDomains
{
	public static class TransactionFacilitation extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionFacilitation INSTANCE = new TransactionFacilitation();
	}

	public static class TransactionElement extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionElement INSTANCE = new TransactionElement();
	}

	public static class TransactionParticipant extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionParticipant INSTANCE = new TransactionParticipant();
	}
}
