package org.hawkinssoftware.azia.core.role;

import org.hawkinssoftware.rns.core.role.DomainRole;

public interface UserInterfaceDomains
{
	public static class AssemblyDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final AssemblyDomain INSTANCE = new AssemblyDomain();
	}

	public static class PainterCompositionDomain extends AssemblyDomain
	{
		@DomainRole.Instance
		public static final PainterCompositionDomain INSTANCE = new PainterCompositionDomain();
	}

	public static class RenderingDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final RenderingDomain INSTANCE = new RenderingDomain();
	}

	public static class DisplayBoundsDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final DisplayBoundsDomain INSTANCE = new DisplayBoundsDomain();
	}

	public static class FlyweightCellDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final FlyweightCellDomain INSTANCE = new FlyweightCellDomain();
	}
}
