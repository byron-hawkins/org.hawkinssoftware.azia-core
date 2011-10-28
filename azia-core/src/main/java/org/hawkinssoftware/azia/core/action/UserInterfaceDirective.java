package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

@DomainRole.Join(membership = TransactionElement.class)
public abstract class UserInterfaceDirective implements UserInterfaceActorDelegate
{
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
