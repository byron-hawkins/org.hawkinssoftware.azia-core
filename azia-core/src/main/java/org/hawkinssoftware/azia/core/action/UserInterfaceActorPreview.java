package org.hawkinssoftware.azia.core.action;

import java.util.List;

public interface UserInterfaceActorPreview
{
	boolean affects(UserInterfaceTransactionQuery.Property<?, ?> property);

	<T> T getPreview(UserInterfaceDirective action, T value);
	
	public interface Host
	{
		List<UserInterfaceActorPreview> getPreviews(UserInterfaceDirective action);		
	}
}
