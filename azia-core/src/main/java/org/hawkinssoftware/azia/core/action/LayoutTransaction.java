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

import org.hawkinssoftware.azia.core.layout.BoundedEntity;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.DomainRole;

// TODO: kind of weird that the ApplyLayout transactions are not of this kind
/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@DomainRole.Join(membership = UserInterfaceTransactionDomains.TransactionElement.class)
@InvocationConstraint(domains = { UserInterfaceTransactionDomains.TransactionFacilitation.class })
public interface LayoutTransaction extends UserInterfaceTransaction
{
	BoundedEntity.LayoutRoot getLayoutRoot();
} 
