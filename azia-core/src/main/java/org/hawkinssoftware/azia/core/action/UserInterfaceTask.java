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

// A UITask is a participant in the sense of operating on transactions, and an element in the sense of being integrated into the transaction execution process.
/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@CollaborationObserver.Initiate
@DomainRole.Join(membership = { TransactionElement.class, TransactionParticipant.class })
public abstract class UserInterfaceTask
{ 
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Type
	{ 
		PROCESSING,
		POST_PROCESSING;    
	} 

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum CollisionStatus    
	{
		CROSSLOCK, 
		TIMEOUT;
	}

	/**
	 * DOC comment task awaits.
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
	 * DOC comment task awaits.
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
