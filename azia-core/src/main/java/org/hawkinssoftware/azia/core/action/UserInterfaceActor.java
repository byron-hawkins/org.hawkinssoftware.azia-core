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
import org.hawkinssoftware.rns.core.validation.ValidateRead;
import org.hawkinssoftware.rns.core.validation.ValidateWrite;

/**
 * By implementing this interface, the UIActor declares itself to be a concrete node in the graph of semaphores which
 * are used to enforce <code>@ValidateRead</code> and <code>@ValidateWrite</code> constraints on fields throughout the
 * application. An <code>@InitializationAspect</code> facilitates assignment of a semaphore in the centralized
 * transaction manager with this Actor when it is instantiated, so the implementor need not be concerned about that.
 * Locked fields of all instances of <code>UserInterfaceActorDelegate</code> which refer to this UIActor will be bound
 * to the one sempahore allocated to this UIActor. Instances of <code>UserInterfaceDirective</code> and
 * <code>UserInterfaceNotification</code> directed to this UIActor will be made available to all delegates of this
 * UIActor and all of their <code>UserInterfaceHandler</code>s.
 * 
 * @author Byron Hawkins
 * @see ValidateRead
 * @see ValidateWrite
 * @see InitializationAspect
 * @see UserInterfaceNotification
 * @see UserInterfaceHandler
 */
@InitializationAspect(agent = UserInterfaceActor.Agent.class)
@InvocationConstraint(domains = { TransactionFacilitation.class }, types = { UserInterfaceDirective.class, UserInterfaceDirective.Notification.class })
@DomainRole.Join(membership = { UserInterfaceActor.ActorDomain.class })
public interface UserInterfaceActor extends ActorBasedContributor, UserInterfaceActorDelegate
{
	void apply(UserInterfaceDirective action);

	/**
	 * Specifies a lock type for a <code>UserInterfaceActor</code>. The Role is held by the member classes of the
	 * <code>InstantiationTask</code> for the duration of its execution, and the centralized transaction manager
	 * observes this value and assigns it to all instances of UIActor instantiated during that period. The Role is most
	 * often specified in the constructor of a <code>ComponentAssembly</code>.
	 * 
	 * @author Byron Hawkins
	 * @see UserInterfaceActor
	 * @see InstantiationTask
	 * @see ComponentAssembly
	 */
	public enum SynchronizationRole
	{
		/**
		 * Indicates to the centralized transaction manager that the most recent lock on the instantiation stack (i.e.,
		 * the call stack during instantiation) should be directly assigned to the associated UIActor. If no lock exists
		 * on the instantiation stack, an exception will be thrown, so this Role should only be assigned to components
		 * like the <code>SliderKnob</code> which are guaranteed to be subcomponents in all instantiation scenarios.
		 * 
		 * @see SliderKnob
		 */
		SUBORDINATE,
		/**
		 * Indicates to the centralized transaction manager that a <code>DependentLock</code> should be used for the
		 * associated UIActor whenever possible. A DependentLock is only available when an <code>AutonomousLock</code>
		 * can be found within the instantiation context (i.e., above it on the call stack during instantiation). If
		 * there is no AutonomousLock to depend on, then the UIActor is allocated its own AutonomousLock.
		 * <p>
		 * This is useful for components that sometimes have the role of sub-component and other times have the role of
		 * standalone component. For example, a slider might participate in a scroll pane as one of the scroll bars, and
		 * in this case it would depend on the scroll pane's AutonomousLock (and itself have a DependentLock); or it
		 * might be a standalone numerical slider, in which case it would prefer to have its own AutonomousLock.
		 * 
		 * @see DependentLock
		 * @see AutonomousLock
		 */
		DEPENDENT,
		/**
		 * Indicates to the centralized transaction manager that an <code>AutonomousLock</code> must be granted to the
		 * associated UIActor. This is used for components which always have the role of top-level component, such as
		 * scroll panes.
		 * 
		 * @see AutonomousLock
		 */
		AUTONOMOUS;
	}

	/**
	 * A DomainRole for use in restricting publication of <code>UserInterfaceActor</code> concerns.
	 * 
	 * @author Byron Hawkins
	 */
	public static class ActorDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final ActorDomain INSTANCE = new ActorDomain();
	}

	/**
	 * Every <code>UserInterfaceActor</code> in the <code>DependentActorDomain</code> is assigned a DependentLock, and
	 * all others are assigned an `AutonomousLock.
	 * 
	 * @author Byron Hawkins
	 * @see DependentLock
	 * @see AutonomousLock
	 */
	public static class DependentActorDomain extends DomainRole
	{
		@DomainRole.Instance
		public static final DependentActorDomain INSTANCE = new DependentActorDomain();
	}

	// TODO: should not need to specify publication constraint allowance for types inside the same file
	/**
	 * Initialization agent facilitating the <code>@InitializationAspect</code> for <code>UserInterfaceActor</code>.
	 * 
	 * @author Byron Hawkins
	 * @see InitializationAspect
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
