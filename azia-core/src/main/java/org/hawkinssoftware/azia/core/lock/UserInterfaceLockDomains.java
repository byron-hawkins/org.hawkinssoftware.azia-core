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

import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.aop.ClassLoadObserver.ClassLoadObservationDomain;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.role.TypeRole;
import org.hawkinssoftware.rns.core.util.RNSUtils;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
interface UserInterfaceLockDomains
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	static class LockManagement extends DomainRole
	{
		@DomainRole.Instance
		public static final LockManagement INSTANCE = new LockManagement();

		/**
		 * DOC comment task awaits.
		 * 
		 * @author Byron Hawkins
		 */
		static class ContainmentConstraint implements ExecutionPath.StackObserver
		{
			static ContainmentConstraint INSTANCE = new ContainmentConstraint();

			private LockAccessValidator.Semaphore semaphore = null;

			void semaphoreAcquired(LockAccessValidator.Semaphore semaphore)
			{
				ExecutionPath.addObserver(INSTANCE);
				this.semaphore = semaphore;
			}

			void semaphoreReleased()
			{
				ExecutionPath.removeObserver(INSTANCE);
			}

			@Override
			public void sendingMessage(TypeRole senderRole, TypeRole receiverRole, Object receiver, String messageDescription)
			{
				if (!(receiverRole.hasRole(LockManagement.INSTANCE) || receiverRole.hasRole(ClassLoadObservationDomain.INSTANCE)))
				{
					Log.out(Tag.LOCK_WARNING, "Warning: attempt to execute code outside the " + RNSUtils.getPlainName(LockManagement.class)
							+ " domain while holding the " + semaphore + " lock.");
				}
			}

			@Override
			public void messageReturningFrom(TypeRole receiverRole, Object receiver)
			{
			}
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	static class ThreadStateValidation extends DomainRole
	{
		@DomainRole.Instance
		public static final ThreadStateValidation INSTANCE = new ThreadStateValidation();
	}
}
