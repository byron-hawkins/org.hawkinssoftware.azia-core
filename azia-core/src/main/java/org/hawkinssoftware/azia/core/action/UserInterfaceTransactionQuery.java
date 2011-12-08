package org.hawkinssoftware.azia.core.action;

import java.util.ArrayList;
import java.util.List;

import org.hawkinssoftware.azia.core.action.UserInterfaceTransactionDomains.TransactionElement;
import org.hawkinssoftware.rns.core.moa.ExecutionContext;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.moa.ExecutionStackFrame;
import org.hawkinssoftware.rns.core.role.DomainRole;

@ExecutionPath.NoFrame
public class UserInterfaceTransactionQuery
{
	private static final ThreadLocal<ModeStack> QUERY_MODE = new ThreadLocal<ModeStack>() {
		@Override
		protected ModeStack initialValue()
		{
			return new ModeStack();
		}
	};

	@ExecutionPath.NoFrame
	private static class ModeStack extends ExecutionContext
	{
		private static final Mode DEFAULT_MODE = Mode.READ_TRANSACTIONAL_CHANGES;

		private final List<Frame> frames = new ArrayList<Frame>();

		private Frame currentFrame = null;

		void changeMode(Mode newMode)
		{
			if (currentFrame == null)
			{
				if (newMode == DEFAULT_MODE)
				{
					return;
				}

				ExecutionPath.installExecutionContext(Key.INSTANCE, this);
			}
			else
			{
				if (currentFrame.mode == newMode)
				{
					return;
				}

				if (currentFrame.invocationDepth == 0)
				{
					currentFrame.mode = newMode;
					return;
				}
			}

			Frame frame = new Frame(newMode);
			frames.add(frame);
			currentFrame = frame;
		}

		Mode getCurrentMode()
		{
			if (currentFrame == null)
			{
				return DEFAULT_MODE;
			}
			else
			{
				return currentFrame.mode;
			}
		}

		@Override
		protected void pushFrame(ExecutionStackFrame frame)
		{
			currentFrame.invocationDepth++;
		}

		@Override
		protected void popFromFrame(ExecutionStackFrame frame)
		{
			if (currentFrame.invocationDepth == 0)
			{
				frames.remove(frames.size() - 1);
				if (frames.isEmpty())
				{
					currentFrame = null;
					ExecutionPath.removeExecutionContext(Key.INSTANCE);
				}
				else
				{
					currentFrame = frames.get(frames.size() - 1);
				}
			}
			else
			{
				currentFrame.invocationDepth--;
			}
		}

		@ExecutionPath.NoFrame
		private class Frame
		{
			Mode mode;
			int invocationDepth = 0;

			Frame(Mode mode)
			{
				this.mode = mode;
			}
		}

		/**
		 * DOC comment task awaits.
		 * 
		 * @author Byron Hawkins
		 */
		@ExecutionPath.NoFrame
		private static final class Key implements ExecutionContext.Key<ModeStack>
		{
			static final Key INSTANCE = new Key();
		}
	}

	public enum Mode
	{
		READ_TRANSACTIONAL_CHANGES,
		IGNORE_TRANSACTIONAL_CHANGES;
	}

	@DomainRole.Join(membership = TransactionElement.class)
	public static final class Node<T>
	{
		final T value;

		Node(T currentValue)
		{
			value = currentValue;
		}

		public <Child> Node<Child> getTransactionalValue(Property<? super T, Child> property)
		{
			Child currentChildValue = property.getCurrentValue(value);

			if (isReadingTransactionalChanges())
			{
				UserInterfaceActor actor = null;
				if (value instanceof UserInterfaceActorDelegate)
				{
					actor = ((UserInterfaceActorDelegate) value).getActor();
				}

				if ((actor != null) && actor.hasPreviews())
				{
					List<UserInterfaceDirective> actions = TransactionRegistry.getActionsOn(actor);
					for (UserInterfaceDirective action : actions)
					{
						List<UserInterfaceActorPreview> previews = actor.getPreviews(action);
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
		QUERY_MODE.get().changeMode(b ? Mode.READ_TRANSACTIONAL_CHANGES : Mode.IGNORE_TRANSACTIONAL_CHANGES);
	}

	public static boolean isReadingTransactionalChanges()
	{
		return QUERY_MODE.get().getCurrentMode() == Mode.READ_TRANSACTIONAL_CHANGES;
	}
}
