package org.hawkinssoftware.azia.core.action;

public interface UserInterfaceActorPreview
{
	boolean affects(UserInterfaceTransactionQuery.Property<?, ?> property);

	<T> T getPreview(UserInterfaceDirective action, T value);
}
