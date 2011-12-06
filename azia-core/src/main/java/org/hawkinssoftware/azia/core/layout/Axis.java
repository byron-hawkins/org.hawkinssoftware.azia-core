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

import java.awt.geom.Rectangle2D;

import org.hawkinssoftware.azia.core.role.UserInterfaceDomains.DisplayBoundsDomain;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.publication.VisibilityConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.util.UnknownEnumConstantException;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@VisibilityConstraint(domains = DisplayBoundsDomain.class)
@DomainRole.Join(membership = DisplayBoundsDomain.class)
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

		public static class Double implements Axis.Bounds
		{
			public Rectangle2D bounds;

			public Double(double x, double y, double width, double height)
			{
				bounds = new Rectangle2D.Double(x, y, width, height);
			}

			public Double(Rectangle2D bounds)
			{
				this.bounds = bounds;
			}

			@Override
			public int getExtent(Axis axis)
			{
				return getPosition(axis) + getExtent(axis);
			}

			@Override
			public int getPosition(Axis axis)
			{
				switch (axis)
				{
					case H:
						return (int) Math.round(bounds.getX());
					case V:
						return (int) Math.round(bounds.getY());
					default:
						throw new UnknownEnumConstantException(axis);
				}
			}

			@Override
			public int getSpan(Axis axis)
			{
				switch (axis)
				{
					case H:
						return (int) Math.round(bounds.getWidth());
					case V:
						return (int) Math.round(bounds.getHeight());
					default:
						throw new UnknownEnumConstantException(axis);
				}
			}

		}
	}

	@DomainRole.Join(membership = DisplayBoundsDomain.class)
	public static class Span
	{
		public final Axis axis;
		public final int position;
		public final int span;

		public Span(Axis axis, int position, int span)
		{
			this.axis = axis;
			this.position = position;
			this.span = span;
		}
	}
}
