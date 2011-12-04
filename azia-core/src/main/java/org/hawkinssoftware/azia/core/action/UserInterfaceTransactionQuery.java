package org.hawkinssoftware.azia.core.action;

import java.util.List;

public class UserInterfaceTransactionQuery
{
	private static final ThreadLocal<ModeContainer> QUERY_MODE = new ThreadLocal<ModeContainer>() {
		@Override
		protected ModeContainer initialValue()
		{
			return new ModeContainer();
		}
	};

	private static class ModeContainer
	{
		private Mode mode = Mode.IGNORE_TRANSACTIONAL_CHANGES;
	}

	public enum Mode
	{
		READ_TRANSACTIONAL_CHANGES,
		IGNORE_TRANSACTIONAL_CHANGES;
	}

	public static class Node<T>
	{
		final T value;

		Node(T currentValue)
		{
			value = currentValue;
		}

		public <Child> Node<Child> getTransactionalValue(Property<? super T, Child> property)
		{
			Child currentChildValue = property.getCurrentValue(value);

			if (readTransactionalChanges())
			{
				UserInterfaceActor actor = null;
				UserInterfaceActorPreview.Host previewHost = null;
				if (value instanceof UserInterfaceActorDelegate)
				{
					actor = ((UserInterfaceActorDelegate) value).getActor();
					if (actor instanceof UserInterfaceActorPreview.Host)
					{
						previewHost = (UserInterfaceActorPreview.Host) actor;
					}
				}

				if (previewHost != null)
				{
					List<UserInterfaceDirective> actions = TransactionRegistry.getActionsOn(actor);
					for (UserInterfaceDirective action : actions)
					{
						List<UserInterfaceActorPreview> previews = previewHost.getPreviews(action);
						for (UserInterfaceActorPreview preview : previews)
						{
							if (preview.affects(property))
							{
								currentChildValue = preview.getPreview(action, currentChildValue);
							}
						}
					}
				}
			}

			return new Node<Child>(currentChildValue);
		}

		public T getValue()
		{
			return value;
		}
	}

	public static abstract class Property<ParentType, ChildType>
	{
		public final String methodName;
		public final Class<?>[] parameters;

		public Property(String methodName, Class<?>... parameters)
		{
			this.methodName = methodName;
			this.parameters = parameters;
		}

		protected abstract ChildType getCurrentValue(ParentType parentValue);

		public final boolean matches(String methodName, Class<?>... parameters)
		{
			if (!this.methodName.equals(methodName))
			{
				return false;
			}

			if (this.parameters.length != parameters.length)
			{
				return false;
			}

			for (int i = 0; i < this.parameters.length; i++)
			{
				if (parameters[i] != this.parameters[i])
				{
					return false;
				}
			}

			return true;
		}
	}

	public static <T> Node<T> start(T queryRoot)
	{
		return new Node<T>(queryRoot);
	}

	public static void setReadTransactionalChanges(boolean b)
	{
		QUERY_MODE.get().mode = b ? Mode.READ_TRANSACTIONAL_CHANGES : Mode.IGNORE_TRANSACTIONAL_CHANGES;
	}

	private static boolean readTransactionalChanges()
	{
		return QUERY_MODE.get().mode == Mode.READ_TRANSACTIONAL_CHANGES;
	}
}
