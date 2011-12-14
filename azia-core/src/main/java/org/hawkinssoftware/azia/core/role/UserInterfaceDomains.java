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
package org.hawkinssoftware.azia.core.role;

import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * Shell interface for the global <code>DomainRole</code>s of the Azia UI Library.
 * 
 * @author Byron Hawkins
 */
public interface UserInterfaceDomains
{
	/**
	 * Domain exclusive to classes participating in the assembly of the application and its compositions.
	 * 
	 * @author Byron Hawkins
	 */
	public static class AssemblyDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final AssemblyDomain INSTANCE = new AssemblyDomain();
	}

	/**
	 * Domain exclusive to the composing of <code>InstancePainter</code>.
	 * 
	 * @author Byron Hawkins
	 */
	public static class PainterCompositionDomain extends AssemblyDomain
	{
		@DomainRole.Instance
		public static final PainterCompositionDomain INSTANCE = new PainterCompositionDomain();
	}

	/**
	 * Domain exclusive to screen rendering.
	 * 
	 * @author Byron Hawkins
	 */
	public static class RenderingDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final RenderingDomain INSTANCE = new RenderingDomain();
	}

	/**
	 * Domain exclusive to the maintenance of displayable bounds.
	 * 
	 * @author Byron Hawkins
	 */
	public static class DisplayBoundsDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final DisplayBoundsDomain INSTANCE = new DisplayBoundsDomain();
	}

	// WIP: what's really the distinction between ModelListDomain and FlyCellDomain?
	/**
	 * Domain exclusive to the integration of flyweights (aka. "stamps") into a collection display.
	 * 
	 * @author Byron Hawkins
	 */
	public static class FlyweightCellDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final FlyweightCellDomain INSTANCE = new FlyweightCellDomain();
	}
}
