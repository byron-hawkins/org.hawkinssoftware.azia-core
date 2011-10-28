package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.rns.core.role.DomainRole;

@DomainRole.Join(membership = TransactionElement.class)
public abstract class UserInterfaceNotification
{
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
