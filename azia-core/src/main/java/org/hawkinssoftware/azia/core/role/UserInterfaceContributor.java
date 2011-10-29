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
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public class UserInterfaceContributor extends DomainRole
{  
	public static final ExecutionPath.StackObserver REQUIRED = new SinglePropertyConstraint<UserInterfaceIntegration>(UserInterfaceIntegration.CONTRIBUTOR,
			SinglePropertyConstraint.REQUIRED_UNIQUE);

	@DomainRole.Instance
	public static final UserInterfaceContributor INSTANCE = new UserInterfaceContributor();

	public UserInterfaceContributor()
	{
		setProperty(UserInterfaceIntegration.class, UserInterfaceIntegration.CONTRIBUTOR);
	}
}
