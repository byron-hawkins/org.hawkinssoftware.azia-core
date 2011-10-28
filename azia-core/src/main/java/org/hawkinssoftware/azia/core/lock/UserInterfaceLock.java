package org.hawkinssoftware.azia.core.lock;

import java.util.concurrent.atomic.AtomicInteger;

import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;

@ExecutionPath.NoFrame
@DomainRole.Join(membership = LockManagement.class)
abstract class UserInterfaceLock
{
	private static final AtomicInteger INDEX = new AtomicInteger();

	private final int index;
	private final String description;

	UserInterfaceLock(String description)
	{
		index = INDEX.incrementAndGet();
		this.description = description;
	}

	abstract AutonomousLock getAutonomousLock();

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "-" + index + " [" + description + "]";
	}
}
