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
package org.hawkinssoftware.azia.core.util;

import java.util.ArrayList;

/**
 * DOC comment task awaits.
 * 
 * @param <E>
 *            the element type
 * @author Byron Hawkins
 */
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
