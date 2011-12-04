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
package org.hawkinssoftware.azia.core.action;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionParticipant;
import org.hawkinssoftware.azia.core.role.CollaborationObserver;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * Universal entry point for starting and joining transactions. Executes on the calling thread. If a transaction is in
 * progress, the task will find it and join; otherwise a new transaction will be started (the caller need not be aware).
 * It is presently not possible to force the creation of a new transaction when the calling thread has one in progress.
 * 
 * @author Byron Hawkins
 */
@CollaborationObserver.Initiate
@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
public abstract class UserInterfaceTask
{
	/**
	 * Specifies the phase of the transaction during which this task should be executed.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Type
	{
		PROCESSING,
		POST_PROCESSING;
	}

	/**
	 * Specifies the particular condition under which a transactional lock acquisition failed.
	 * 
	 * @author Byron Hawkins
	 */
	public enum CollisionStatus
	{
		/**
		 * Typical deadlock condition: mutual attempt to acquire held resources.
		 */
		CROSSLOCK,
		/**
		 * Lock acquisition timeout.
		 */
		TIMEOUT;
	}

	/**
	 * Thrown when a group of transactions has collided with another and has been selected for termination and retry.
	 * 
	 * @author Byron Hawkins
	 */
	protected static final class RetryException extends RuntimeException
	{
		@InvocationConstraint(packages = InvocationConstraint.MY_PACKAGE)
		public RetryException()
		{
		}
	}

	/**
	 * Thrown when a group of transactions has been already been retried the maximum number of times, and it has once
	 * again been selected for termination and retry. The transaction client will receive this as an indication of
	 * transaction failure.
	 * 
	 * @author Byron Hawkins
	 */
	public static class ConcurrentAccessException extends Exception
	{
		@InvocationConstraint(packages = InvocationConstraint.MY_PACKAGE)
		public ConcurrentAccessException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	final Type type;
	private UserInterfaceTransactionSession session;
	private int maximumRetryCount = 10;

	protected UserInterfaceTask()
	{
		this(Type.PROCESSING);
	}

	protected UserInterfaceTask(Type type)
	{
		this.type = type;
	}

	@InvocationConstraint(packages = InvocationConstraint.MY_PACKAGE)
	protected abstract boolean execute();

	protected void setRetryCount(int maximumRetryCount)
	{
		this.maximumRetryCount = maximumRetryCount;
	}

	public void start() throws ConcurrentAccessException
	{
		if (session == null)
		{
			TransactionRegistry.executeTask(this);
		}
	}

	public int getRetryCount()
	{
		return maximumRetryCount;
	}

	void setSession(UserInterfaceTransactionSession session)
	{
		this.session = session;
	}

	@SuppressWarnings("unchecked")
	protected final <TransactionType extends UserInterfaceTransaction> TransactionType getTransaction(Class<TransactionType> transactionType)
	{
		return (TransactionType) session.joinSession(transactionType);
	}

	@InvocationConstraint(packages = InvocationConstraint.MY_PACKAGE)
	final void returnAndRetry(CollisionStatus status)
	{
		System.err.println("Failing task " + getClass().getSimpleName() + " on thread " + Thread.currentThread().getName() + " for " + status);
		throw new RetryException();
	}
}
