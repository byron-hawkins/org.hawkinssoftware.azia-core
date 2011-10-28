package org.hawkinssoftware.azia.core.layout;

import org.hawkinssoftware.azia.core.role.UserInterfaceDomains.DisplayBoundsDomain;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.publication.VisibilityConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

@VisibilityConstraint(domains = DisplayBoundsDomain.class)
public enum Axis
{
	H
	{
		@Override
		public Axis opposite()
		{
			return V;
		}

		public int extractPosition(ScreenPosition position)
		{
			return position.x();
		}
	},
	V
	{
		@Override
		public Axis opposite()
		{
			return H;
		}

		@Override
		public int extractPosition(ScreenPosition position)
		{
			return position.y();
		}
	};

	public abstract int extractPosition(ScreenPosition position);

	public abstract Axis opposite();

	@InvocationConstraint(domains = DisplayBoundsDomain.class)
	@DomainRole.Join(membership = DisplayBoundsDomain.class)
	public interface Bounds
	{
		int getPosition(Axis axis);

		int getExtent(Axis axis);

		int getSpan(Axis axis);
	}
}
