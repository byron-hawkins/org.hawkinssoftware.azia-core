package org.hawkinssoftware.azia.core.lock;

import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * The state of the owner of this DependentLock is directly applied to this lock's domain.
 * 
 * @author b
 */
@ExecutionPath.NoFrame
@DomainRole.Join(membership = LockManagement.class)
class DependentLock extends UserInterfaceLock
{
	private final UserInterfaceLock owner;

	DependentLock(UserInterfaceLock owner, String description)
	{
		super(description);

		this.owner = owner;
	}

	@Override
	public AutonomousLock getAutonomousLock()
	{
		UserInterfaceLock lock = owner;
		while (true)
		{
			if (lock == null)
			{
				throw new IllegalStateException("Dependent lock is associated to no autonomous lock!");
			}

			if (lock instanceof AutonomousLock)
			{
				break;
			}

			if (lock instanceof DependentLock)
			{
				lock = ((DependentLock) lock).owner;
			}
			else
			{
				throw new IllegalStateException("Unknown lock type " + lock.getClass().getName());
			}
		}
		return (AutonomousLock) lock;
	}
}
