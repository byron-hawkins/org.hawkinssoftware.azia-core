package org.hawkinssoftware.azia.core.role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkinssoftware.azia.core.log.AziaLogging.Tag;
import org.hawkinssoftware.rns.core.log.Log;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;
import org.hawkinssoftware.rns.core.role.TypeRole;

@ExecutionPath.NoFrame
public class CollaborationObserver implements ExecutionPath.StackObserver
{
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Initiate
	{
	}

	@ExecutionPath.NoFrame
	public static class Factory implements ExecutionPath.StackObserver.Factory<CollaborationObserver>
	{
		@Override
		public CollaborationObserver create()
		{
			return new CollaborationObserver();
		}

		public java.lang.Class<? extends CollaborationObserver> getObserverType()
		{
			return CollaborationObserver.class;
		}
	}

	@ExecutionPath.NoFrame
	private static class DomainFootprint
	{
		final Object initiator;
		final Set<DomainRole> collaboratingDomains = new HashSet<DomainRole>();

		int stackDepth = 0;
		int maxStackDepth = 0;

		DomainFootprint(Object initiator)
		{
			this.initiator = initiator;
		}

		void incrementStack()
		{
			stackDepth++;
			maxStackDepth = Math.max(maxStackDepth, stackDepth);
		}

		void decrementStack()
		{
			stackDepth--;
		}
	}

	private final Map<Object, DomainFootprint> activeFootprintsByReceiver = new HashMap<Object, DomainFootprint>();
	private final Map<Class<?>, Boolean> initiators = new HashMap<Class<?>, Boolean>();

	@Override
	public void sendingMessage(TypeRole senderRole, TypeRole receiverRole, Object receiver, String messageDescription)
	{
		if (receiver instanceof Class)
		{
			return;
		}

		boolean isConstructor = ((messageDescription != null) && messageDescription.contains("<"));
		if ((!isConstructor) && isInitiator(receiverRole))
		{
			if (!activeFootprintsByReceiver.containsKey(receiver))
			{
				activeFootprintsByReceiver.put(receiver, new DomainFootprint(receiver));
			}
		}

		for (DomainFootprint footprint : activeFootprintsByReceiver.values())
		{
			footprint.incrementStack();
			footprint.collaboratingDomains.addAll(receiverRole.getMembership());
		}
	}

	@Override
	public void messageReturningFrom(TypeRole receiverRole, Object receiver)
	{
		if (receiver instanceof Class)
		{
			return;
		}

		for (Object initiator : new ArrayList<Object>(activeFootprintsByReceiver.keySet()))
		{
			DomainFootprint footprint = activeFootprintsByReceiver.get(initiator);
			footprint.decrementStack();
			if (footprint.stackDepth == 0)
			{
				activeFootprintsByReceiver.remove(initiator);

				if (footprint.maxStackDepth > 1)
				{
					reportCollaboration(footprint);
				}
			}
		}
	}

	private void reportCollaboration(DomainFootprint footprint)
	{
		StringBuilder buffer = new StringBuilder();

		List<String> collaboratingDomainNames = new ArrayList<String>();
		for (DomainRole role : footprint.collaboratingDomains)
		{
			collaboratingDomainNames.add(role.getClass().getSimpleName());
		}
		Collections.sort(collaboratingDomainNames);
		for (String collaboratingDomainName : collaboratingDomainNames)
		{
			buffer.append("\n   ");
			buffer.append(collaboratingDomainName);
		}

		String classname;
		Class<?> initiatorClass = footprint.initiator.getClass();
		if (initiatorClass.isAnonymousClass())
		{
			classname = "[Anonymous " + initiatorClass.getSuperclass().getSimpleName() + " in " + initiatorClass.getEnclosingClass().getSimpleName() + "."
					+ initiatorClass.getEnclosingMethod().getName() + "()]";
		}
		else
		{
			classname = footprint.initiator.getClass().getSimpleName();
		}

		Log.out(Tag.DEBUG, "Collaborations for %s: %s", classname, buffer.toString());
	}

	private boolean isInitiator(TypeRole role)
	{
		Boolean isInitiator = initiators.get(role.getType());
		if (isInitiator == null)
		{
			isInitiator = isInitiator(role.getType());
			initiators.put(role.getType(), isInitiator);
		}
		return isInitiator;
	}

	private boolean isInitiator(Class<?> type)
	{
		if (type == null)
		{
			return false;
		}

		if (type.getAnnotation(Initiate.class) != null)
		{
			return true;
		}

		if (isInitiator(type.getSuperclass()))
		{
			return true;
		}

		for (Class<?> implemented : type.getInterfaces())
		{
			if (isInitiator(implemented))
			{
				return true;
			}
		}

		return false;
	}
}
