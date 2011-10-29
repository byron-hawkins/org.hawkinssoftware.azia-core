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
package org.hawkinssoftware.azia.core.lock;

import java.util.concurrent.atomic.AtomicInteger;

import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
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
