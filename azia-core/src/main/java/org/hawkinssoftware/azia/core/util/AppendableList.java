package org.hawkinssoftware.azia.core.util;

import java.util.ArrayList;

public class AppendableList<E> extends ArrayList<E> implements AppendableCollection<E>
{
	public void append(E element)
	{
		if (element == null)
		{
			return;
		}
		super.add(element);
	}
}
