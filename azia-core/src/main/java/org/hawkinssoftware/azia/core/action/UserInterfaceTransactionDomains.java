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
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public interface UserInterfaceTransactionDomains
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionFacilitation extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionFacilitation INSTANCE = new TransactionFacilitation();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionElement extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionElement INSTANCE = new TransactionElement();
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class TransactionParticipant extends DomainRole
	{
		@DomainRole.Instance
		public static final TransactionParticipant INSTANCE = new TransactionParticipant();
	}
}
