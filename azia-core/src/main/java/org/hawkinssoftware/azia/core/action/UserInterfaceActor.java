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

import org.hawkinssoftware.azia.core.action.UserInterfaceTransaction.ActorBasedContributor;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.azia.core.layout.BoundedEntity.LayoutContainerDomain;
import org.hawkinssoftware.azia.core.lock.LockRegistry;
import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.aop.InitializationAspect;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.publication.VisibilityConstraint;
import org.hawkinssoftware.rns.core.role.CoreDomains.InitializationDomain;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.role.RoleRegistry;
import org.hawkinssoftware.rns.core.role.TypeRole;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
@InitializationAspect(agent = UserInterfaceActor.Agent.class)
@InvocationConstraint(domains = { TransactionFacilitation.class }, types = { UserInterfaceDirective.class, UserInterfaceDirective.Notification.class })
@DomainRole.Join(membership = { UserInterfaceActor.ActorDomain.class })
public interface UserInterfaceActor extends ActorBasedContributor, UserInterfaceActorDelegate
{ 
	void apply(UserInterfaceDirective action);

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum SynchronizationRole
	{
		SUBORDINATE,
		DEPENDENT,
		AUTONOMOUS;
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class ActorDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final ActorDomain INSTANCE = new ActorDomain();
	}

	/**
	 * Every `UserInterfaceActor in the `DependentActorDomain is assigned a `DependentLock, and all others are assigned
	 * an `AutonomousLock.
	 * 
	 * @author b
	 */
	public static class DependentActorDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final DependentActorDomain INSTANCE = new DependentActorDomain();
	}

	// TODO: should not need to specify same file access
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@VisibilityConstraint(extendedTypes = UserInterfaceActor.class)
	@DomainRole.Join(membership = { TransactionFacilitation.class, InitializationDomain.class })
	public static class Agent implements InitializationAspect.Agent<UserInterfaceActor>
	{
		public static final Agent INSTANCE = new Agent();

		@Override
		public void initialize(UserInterfaceActor actor)
		{
			TransactionRegistry.getInstance().addActorBasedContributor(actor, actor);

			try
			{
				TypeRole actorRole = RoleRegistry.getRole(actor.getClass());
				if (actorRole.hasRole(DomainRole.Resolver.getInstance(LayoutContainerDomain.class)))
				{
					LayoutTransaction transaction = ExecutionPath.getMostRecentCaller(LayoutTransaction.class);
					if (transaction == null)
					{
						throw new IllegalStateException("Attempt to construct a layout actor outside of a layout transaction.");
					}
					LockRegistry.getInstance().registerLayoutActor(actor, transaction);
				}
				else
				{
					boolean isHandler = actorRole.hasRole(DomainRole.Resolver.getInstance(DependentActorDomain.class));
					// actor.getClass().getAnnotation(Handler.class) != null;
					LockRegistry.getInstance().registerActor(actor, isHandler);
				}
			}
			catch (Exception e)
			{
				Log.out(Tag.CRITICAL, e, "Failed to identify the type roles for actor %s", actor.getClass().getName());
			}
		}
	}
}
