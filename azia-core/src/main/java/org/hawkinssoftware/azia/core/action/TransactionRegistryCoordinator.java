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

import org.hawkinssoftware.azia.core.action.TransactionRegistry.Listener;
 
/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public class TransactionRegistryCoordinator
{
	public static TransactionRegistryCoordinator getInstance()
	{
		return INSTANCE;
	}
	
	private static final TransactionRegistryCoordinator INSTANCE = new TransactionRegistryCoordinator();
	
	private final List<Listener> listeners = new ArrayList<Listener>();
	private final List<UserInterfaceTransaction.PostProcessor> postProcessors = new ArrayList<UserInterfaceTransaction.PostProcessor>();

	public void addListener(Listener listener)
	{
		synchronized (listeners)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(Listener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	// WIP: lousy list copy
	List<Listener> getListeners()
	{
		synchronized (listeners)
		{
			return new ArrayList<Listener>(listeners);
		}
	}

	// WIP: lousy list copy
	List<UserInterfaceTransaction.PostProcessor> getPostProcessors()
	{
		synchronized (postProcessors)
		{
			return new ArrayList<UserInterfaceTransaction.PostProcessor>(postProcessors);
		}
	}

	public void addPostProcessor(UserInterfaceTransaction.PostProcessor postProcessor)
	{
		synchronized (postProcessors)
		{
			postProcessors.add(postProcessor);
		}
	}

	public void removePostProcessor(UserInterfaceTransaction.PostProcessor postProcessor)
	{
		synchronized (postProcessors)
		{
			postProcessors.remove(postProcessor);
		}
	}
}
