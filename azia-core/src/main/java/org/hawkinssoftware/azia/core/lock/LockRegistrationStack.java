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

import java.util.Stack;

import org.hawkinssoftware.azia.core.action.UserInterfaceActor;
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
class LockRegistrationStack
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	private static class InstantiationFrame
	{
		final UserInterfaceLock lock;
		int depth = 0;

		public InstantiationFrame(UserInterfaceLock lock)
		{
			this.lock = lock;
		}
	}

	private final Stack<InstantiationFrame> stack = new Stack<InstantiationFrame>();

	UserInterfaceLock peek()
	{
		return stack.peek().lock;
	}
	
	UserInterfaceLock attachDependent(String description)
	{
		DependentLock lock = new DependentLock(stack.peek().lock, description);
		return lock;
	}

	boolean isEmpty()
	{
		return stack.isEmpty();
	}
	
	void push(UserInterfaceActor.SynchronizationRole actorType, String description)
	{
		switch (actorType)
		{
			case AUTONOMOUS:
				push(new AutonomousLock(description));
				break;
			case DEPENDENT:
				if (stack.isEmpty())
				{
					push(new AutonomousLock(description));
				}
				else
				{
					DependentLock lock = new DependentLock(stack.peek().lock, description);
					push(lock);
				}
				break;
			case SUBORDINATE:
				if (stack.isEmpty())
				{
					throw new IllegalStateException("Attempt to instantiate an actor with UserInterfaceActor.SynchronizationRole of " + actorType
							+ " and no available owner.");
				}
				stack.peek().depth++;
				break;
		}
	}

	void push(UserInterfaceLock lock)
	{
		stack.push(new InstantiationFrame(lock));
	}

	void pop()
	{
		InstantiationFrame frame = stack.peek();
		if (frame.depth > 0)
		{
			frame.depth--;
		}
		else
		{
			stack.pop();
		}
	}
}
