package org.hawkinssoftware.azia.core.util;

public interface AppendableCollection<E>
{
	// null => no-op
	void append(E element);
}
