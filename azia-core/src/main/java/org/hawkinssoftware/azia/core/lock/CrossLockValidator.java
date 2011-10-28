package org.hawkinssoftware.azia.core.lock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkinssoftware.azia.core.lock.LockTransactionContext.LockState;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.ThreadStateValidation;
import org.hawkinssoftware.rns.core.lock.HookSemaphores;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;

// TODO: consider @ExecutionPath.NoFrame on everything in this package (introduce config file?)

@ExecutionPath.NoFrame
@DomainRole.Join(membership = { ThreadStateValidation.class, LockManagement.class })
@HookSemaphores(hook = LockAccessValidator.class, instance = "getInstance()")
class CrossLockValidator
{
	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = { ThreadStateValidation.class, LockManagement.class })
	@HookSemaphores(hook = LockAccessValidator.class, instance = "getInstance()")
	private static class WaitingThread
	{
		final LockTransactionContext context;
		WaitingThread blocker;

		public WaitingThread(LockTransactionContext context)
		{
			this.context = context;
		}

		void findBlocker(List<WaitingThread> waiters)
		{
			blocker = null;
			for (WaitingThread waiter : waiters)
			{
				if (waiter == this)
				{
					continue;
				}

				if (waiter.context.getFullLocks().contains(context.awaitedLock))
				{
					blocker = waiter;
					break;
				}
				if (waiter.context.readOnlyLocks.contains(context.awaitedLock))
				{
					blocker = waiter;
					break;
				}
			}
		}
	}

	static LockState checkCrossLock(AutonomousLock busyLock)
	{
		synchronized (LockTransactionContext.class)
		{
			Collection<LockTransactionContext> contexts = LockTransactionContext.PerThread.getAll();
			List<WaitingThread> waiters = new ArrayList<WaitingThread>();
			for (LockTransactionContext context : contexts)
			{
				if (context.isTransactionActive() && context.isWaiting())
				{
					waiters.add(new WaitingThread(context));
				}
			}
			if (waiters.size() < 2)
			{
				return LockState.OK;
			}

			for (WaitingThread waiter : waiters)
			{
				waiter.findBlocker(waiters);
			}
			for (int i = waiters.size() - 1; i >= 0; i--)
			{
				if (waiters.get(i).blocker == null)
				{
					waiters.remove(i);
				}
			}
			if (waiters.size() < 2)
			{
				return LockState.OK;
			}

			Map<LockTransactionContext, WaitingThread> waitersByContext = new HashMap<LockTransactionContext, WaitingThread>();
			for (WaitingThread waiter : waiters)
			{
				waitersByContext.put(waiter.context, waiter);
			}

			List<WaitingThread> circuit = new ArrayList<WaitingThread>();
			List<WaitingThread> victims = new ArrayList<WaitingThread>();
			do
			{
				WaitingThread initial = waitersByContext.values().iterator().next();
				circuit.add(initial);
				waitersByContext.remove(initial.context);

				while (true)
				{
					WaitingThread blocker = waitersByContext.remove(circuit.get(circuit.size() - 1).blocker.context);
					if (blocker == null)
					{
						System.err.println("Warning: blocker not found for waiter!");
					}
					circuit.add(blocker);
					if (circuit.contains(blocker.blocker))
					{
						break;
					}
				}

				victims.add(circuit.get(0));
				circuit.clear();
			}
			while (!waitersByContext.isEmpty());

			LockState result = LockState.OK;
			Thread currentThread = Thread.currentThread();
			for (WaitingThread victim : victims)
			{
				if (victim.context.thread == currentThread)
				{
					result = LockState.COLLISION;
				}
				else
				{
					victim.context.interrupted = true;
					victim.context.thread.interrupt();
				}
			}
			return result;
		}
	}
}
