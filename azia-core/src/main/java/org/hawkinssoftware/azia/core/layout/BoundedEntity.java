package org.hawkinssoftware.azia.core.layout;

import org.hawkinssoftware.azia.core.action.UserInterfaceNotification;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.role.UserInterfaceDomains.DisplayBoundsDomain;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

@InvocationConstraint(domains = DisplayBoundsDomain.class)
@DomainRole.Join(membership = { TransactionParticipant.class, DisplayBoundsDomain.class })
public interface BoundedEntity
{
	public interface PanelRegion extends BoundedEntity
	{
	}

	public interface LayoutRoot extends PanelRegion
	{
	}
	
	public static class LayoutContainerDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final LayoutContainerDomain INSTANCE = new LayoutContainerDomain();
	}

	public enum Expansion
	{
		FILL,
		FIT;
	}

	public static class MaximumSize
	{
		public static final MaximumSize NONE = new MaximumSize(false);

		private final boolean exists;
		private int value;

		public MaximumSize(boolean exists)
		{
			this.exists = exists;
		}

		public MaximumSize(int value)
		{
			this.exists = true;
			this.value = value;
		}

		public boolean exists()
		{
			return exists;
		}

		public int getValue()
		{
			return value;
		}

		public void setValue(int value)
		{
			if (!exists)
			{
				throw new IllegalStateException("Attempt to set the value on a non-existent MaximumSize");
			}
			this.value = value;
		}
	}

	@InvocationConstraint(domains = DisplayBoundsDomain.class)
	@DomainRole.Join(membership = DisplayBoundsDomain.class)
	public abstract class PositionNotification extends UserInterfaceNotification
	{
		public abstract Integer getPosition(Axis axis);
	}

	Expansion getExpansion(Axis axis);

	int getPackedSize(Axis axis);

	MaximumSize getMaxSize(Axis axis);
}
