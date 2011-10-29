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
package org.hawkinssoftware.azia.core.layout;

import org.hawkinssoftware.azia.core.role.UserInterfaceDomains.DisplayBoundsDomain;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.publication.VisibilityConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
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

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@InvocationConstraint(domains = DisplayBoundsDomain.class)
	@DomainRole.Join(membership = DisplayBoundsDomain.class)
	public interface Bounds
	{
		int getPosition(Axis axis);

		int getExtent(Axis axis);

		int getSpan(Axis axis);
	}
}
