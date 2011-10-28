package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.layout.BoundedEntity;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

// TODO: kind of weird that the ApplyLayout transactions are not of this kind
@DomainRole.Join(membership = UserInterfaceTransactionDomains.TransactionElement.class)
@InvocationConstraint(domains = { UserInterfaceTransactionDomains.TransactionFacilitation.class })
public interface LayoutTransaction extends UserInterfaceTransaction
{
	BoundedEntity.LayoutRoot getLayoutRoot();
} 
