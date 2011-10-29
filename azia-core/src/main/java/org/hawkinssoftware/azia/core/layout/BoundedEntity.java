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

import org.hawkinssoftware.azia.core.action.UserInterfaceNotification;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.role.UserInterfaceDomains.DisplayBoundsDomain;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@InvocationConstraint(domains = DisplayBoundsDomain.class)
@DomainRole.Join(membership = { TransactionParticipant.class, DisplayBoundsDomain.class })
public interface BoundedEntity
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public interface PanelRegion extends BoundedEntity
	{
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public interface LayoutRoot extends PanelRegion
	{
	}
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class LayoutContainerDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final LayoutContainerDomain INSTANCE = new LayoutContainerDomain();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Expansion
	{
		FILL,
		FIT;
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
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

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
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
