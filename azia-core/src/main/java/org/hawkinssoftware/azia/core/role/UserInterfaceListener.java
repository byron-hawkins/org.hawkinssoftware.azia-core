package org.hawkinssoftware.azia.core.role;

import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.util.SinglePropertyConstraint;
  
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
