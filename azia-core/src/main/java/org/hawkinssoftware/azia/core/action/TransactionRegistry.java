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

import java.util.ArrayList;
import java.util.List;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransaction.ActorBasedContributor;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionFacilitation;
import org.hawkinssoftware.rns.core.aop.InitializationAspect;
import org.hawkinssoftware.rns.core.publication.InvocationConstraint;
import org.hawkinssoftware.rns.core.role.CoreDomains.InitializationDomain;
import org.hawkinssoftware.rns.core.role.DomainRole;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Public entry point for executing <code>UserInterfaceTask</code>s. At present the TransactionRegistry should not be
 * used for anything else.
 * 
 * @author Byron Hawkins
 * @see UserInterfaceTask
 */
@DomainRole.Join(membership = TransactionFacilitation.class)
public class TransactionRegistry
{
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@InitializationAspect(agent = ListenerInitializationAgent.class)
	@InvocationConstraint(domains = TransactionFacilitation.class)
	@DomainRole.Join(membership = TransactionElement.class)
	public interface Listener
	{
		Class<? extends UserInterfaceTransaction> transactionInitiated(Class<? extends UserInterfaceTransaction> type);
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@DomainRole.Join(membership = InitializationDomain.class)
	public static class ListenerInitializationAgent implements InitializationAspect.Agent<Listener>
	{
		public static final ListenerInitializationAgent INSTANCE = new ListenerInitializationAgent();

		@Override
		public void initialize(Listener listener)
		{
			TransactionRegistryCoordinator.getInstance().addListener(listener);
		}
	}

	private static final ThreadLocal<UserInterfaceTransactionSession> SESSIONS = new ThreadLocal<UserInterfaceTransactionSession>() {
		@Override
		protected UserInterfaceTransactionSession initialValue()
		{
			return new UserInterfaceTransactionSession();
		}
	};

	public static void executeTask(UserInterfaceTask task) throws UserInterfaceTask.ConcurrentAccessException
	{
		// System.out.println("Execute " + task.getClass().getSimpleName());

		SESSIONS.get().executeTask(task);
	}

	@InvocationConstraint(domains = TransactionFacilitation.class)
	public static void failCurrentTask(UserInterfaceTask.CollisionStatus status)
	{
		SESSIONS.get().failCurrentTask(status);
	}
	
	static List<UserInterfaceDirective> getActionsOn(UserInterfaceActor actor)
	{
		return SESSIONS.get().getActionsOn(actor);
	}

	private static TransactionRegistry INSTANCE;

	public static TransactionRegistry getInstance()
	{
		synchronized (TransactionRegistry.class)
		{
			if (INSTANCE == null)
			{
				INSTANCE = new TransactionRegistry();
			}
			return INSTANCE;
		}
	}

	private final Multimap<UserInterfaceActor, ActorBasedContributor> actorBasedContributors = HashMultimap.create();

	List<ActorBasedContributor> getActorBasedContributors(UserInterfaceActor actor)
	{
		synchronized (actorBasedContributors)
		{
			// TODO: not so efficient here...
			return new ArrayList<ActorBasedContributor>(actorBasedContributors.get(actor));
		}
	}

	public void addActorBasedContributor(UserInterfaceActor actor, ActorBasedContributor contributor)
	{
		synchronized (actorBasedContributors)
		{
			actorBasedContributors.put(actor, contributor);
		}
	}

	public void removeActorBasedContributor(UserInterfaceActor actor)
	{
		synchronized (actorBasedContributors)
		{
			actorBasedContributors.removeAll(actor);
		}
	}

	public void removeActorBasedContributor(UserInterfaceActor actor, ActorBasedContributor contributor)
	{
		synchronized (actorBasedContributors)
		{
			actorBasedContributors.remove(actor, contributor);
		}
	}
}
