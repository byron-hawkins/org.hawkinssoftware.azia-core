package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.rns.core.publication.ExtensionConstraint;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
@InvocationConstraint(domains = { TransactionFacilitation.class, TransactionElement.class })
@ExtensionConstraint(domains = TransactionParticipant.class)
public interface UserInterfaceActorDelegate
{
	UserInterfaceActor getActor();
}
