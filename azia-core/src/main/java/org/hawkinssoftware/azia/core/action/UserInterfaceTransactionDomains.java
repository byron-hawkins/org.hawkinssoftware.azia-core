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

import org.hawkinssoftware.rns.core.role.DomainRole;

/**
 * Shell interface for <code>DomainRole</code>s related to transactions.
 * 
 * @author Byron Hawkins
 */
public interface UserInterfaceTransactionDomains
{

	/**
	 * Domain exclusive to the Azia UI Library's internal implementation of transaction processing.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionFacilitation extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionFacilitation INSTANCE = new TransactionFacilitation();
	}

	/**
	 * Domain incorporating all types which may be collected into a transaction or its session.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionElement extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionElement INSTANCE = new TransactionElement();
	}

	/**
	 * Domain incorporating all classes which contact the <code>TransactionFacilitation</code> domain.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionParticipant extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionParticipant INSTANCE = new TransactionParticipant();
	}
}
