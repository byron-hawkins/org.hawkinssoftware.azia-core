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

import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.util.SinglePropertyConstraint;
  
/**
 * The listener interface for receiving userInterface events. The class that is interested in processing a userInterface
 * event implements this interface, and the object created with that class is registered with a component using the
 * component's <code>addUserInterfaceListener<code> method. When
 * the userInterface event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see UserInterfaceEvent
 */
public class UserInterfaceListener extends DomainRole
{
	public static final ExecutionPath.StackObserver REQUIRED = new SinglePropertyConstraint<UserInterfaceIntegration>(UserInterfaceIntegration.LISTENER,
			SinglePropertyConstraint.REQUIRED_UNIQUE);

	@DomainRole.Instance
	public static final UserInterfaceListener INSTANCE = new UserInterfaceListener();

	public UserInterfaceListener()
	{
		setProperty(UserInterfaceIntegration.class, UserInterfaceIntegration.LISTENER);
	}
}
