package org.hawkinssoftware.azia.core.role;

import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.util.SinglePropertyConstraint;

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
