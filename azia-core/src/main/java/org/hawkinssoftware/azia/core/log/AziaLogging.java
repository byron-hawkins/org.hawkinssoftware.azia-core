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
package org.hawkinssoftware.azia.core.log;

import org.hawkinssoftware.rns.core.log.LogTag;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;

/**
 * DOC comment task awaits.
 * 
 * @author Byron Hawkins
 */
public class AziaLogging
{
	
	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	@ExecutionPath.NoFrame
	public enum LogCategory implements LogTag.Category
	{
		MODE(Mode.class),
		SUBSYSTEM(Subsystem.class),
		TASK(Task.class);

		private final Class<? extends Enum<?>> tokenType;

		private LogCategory(Class<? extends Enum<?>> tokenType)
		{
			this.tokenType = tokenType;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Class<Enum> getTokenType()
		{
			return (Class) tokenType;
		}
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Mode
	{
		FATAL,
		CRITICAL,
		WARNING,
		INFO,
		DEBUG,
		NIT;
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Subsystem
	{
		LOCK,
		HANDLER,
		CANVAS;
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Task
	{
		ROUTER_INIT,
		CONTAINMENT;
	}

	/**
	 * DOC comment task awaits.
	 * 
	 * @author Byron Hawkins
	 */
	public static class Tag
	{
		// WIP: special handling of logging with the ExecutionPath, or something. No frames in the logger, ever.
		/**
		 * DOC comment task awaits.
		 * 
		 * @author Byron Hawkins
		 */
		@ExecutionPath.NoFrame
		private static class DebugWithNoSubsystems extends LogTag<LogCategory>
		{
			public DebugWithNoSubsystems()
			{
				super(LogCategory.class);
			}

			protected void initialize()
			{
				put(Mode.FATAL);
				put(Mode.CRITICAL);
				put(Mode.WARNING);
				put(Mode.INFO);
				put(Mode.DEBUG);

			}

			public boolean includes(LogTag<LogCategory> tag)
			{
				if (!tag.get(Subsystem.class).isEmpty())
				{
					return false;
				}
				return super.includes(tag);
			}
		}

		public static final LogTag<LogCategory> NO_SUBSYSTEMS_UP_TO_DEBUG = new DebugWithNoSubsystems();
		
		public static final LogTag<LogCategory> ALL_SUBSYSTEMS_UP_TO_DEBUG = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.FATAL);
				put(Mode.CRITICAL);
				put(Mode.WARNING);
				put(Mode.INFO);
				put(Mode.DEBUG);
			}
		};
		public static final LogTag<LogCategory> CONTAIN_DEBUG = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.FATAL);
				put(Mode.CRITICAL);
				put(Mode.WARNING);
				put(Mode.INFO);
				put(Mode.DEBUG);
			}

			public boolean includes(LogTag<LogCategory> tag)
			{
				if (!tag.get(Task.class).contains(Task.CONTAINMENT))
				{
					return false;
				}
				return super.includes(tag);
			}
		};

		public static final LogTag<LogCategory> NIT = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.NIT);
			}
		};
		public static final LogTag<LogCategory> DEBUG = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.DEBUG);
			}
		};
		public static final LogTag<LogCategory> DEBUG_CONTAIN = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.DEBUG);
				put(Task.CONTAINMENT);
			}
		};
		public static final LogTag<LogCategory> WARNING = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.WARNING);
			}
		};
		public static final LogTag<LogCategory> CRITICAL = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.CRITICAL);
			}
		};

		public static final LogTag<LogCategory> CANVAS_DEBUG = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.DEBUG);
				put(Subsystem.CANVAS);
			}
		};
		public static final LogTag<LogCategory> ROUTER_INIT = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Task.ROUTER_INIT);
			}
		};
		public static final LogTag<LogCategory> LOCK_WARNING = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.WARNING);
				put(Subsystem.LOCK);
			}
		};
		public static final LogTag<LogCategory> HANDLER_FAIL = new LogTag<LogCategory>(LogCategory.class) {
			protected void initialize()
			{
				put(Mode.FATAL);
				put(Subsystem.HANDLER);
			}
		};
	}
}
