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
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public interface UserInterfaceDomains
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class AssemblyDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final AssemblyDomain INSTANCE = new AssemblyDomain();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class PainterCompositionDomain extends AssemblyDomain
	{
		@DomainRole.Instance
		public static final PainterCompositionDomain INSTANCE = new PainterCompositionDomain();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class RenderingDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final RenderingDomain INSTANCE = new RenderingDomain();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class DisplayBoundsDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final DisplayBoundsDomain INSTANCE = new DisplayBoundsDomain();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class FlyweightCellDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final FlyweightCellDomain INSTANCE = new FlyweightCellDomain();
	}
}
