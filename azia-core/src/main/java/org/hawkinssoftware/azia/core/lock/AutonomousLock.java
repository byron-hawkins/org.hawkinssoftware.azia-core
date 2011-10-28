package org.hawkinssoftware.azia.core.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hawkinssoftware.azia.core.lock.UserInterfaceLockDomains.LockManagement;
import org.hawkinssoftware.rns.core.lock.HookedLock;
import org.hawkinssoftware.rns.core.lock.HookedReadWriteLock;
import org.hawkinssoftware.rns.core.lock.LockHook;
import org.hawkinssoftware.rns.core.moa.ExecutionPath;
import org.hawkinssoftware.rns.core.role.DomainRole;

@ExecutionPath.NoFrame
@DomainRole.Join(membership = LockManagement.class)
class AutonomousLock extends UserInterfaceLock
{
	enum RequestMode
	{
		IMMEDIATE,
		WAIT;
	}

	enum Result
	{
		SUCCESS,
		BUSY,
		INTERRUPTED,
		TIMEOUT;
	}

	enum Level
	{
		READ,
		ASSEMBLY,
		COMMIT;
	}

	@ExecutionPath.NoFrame
	@DomainRole.Join(membership = LockManagement.class)
	private class Hook implements LockHook
	{
		@Override
		public void attemptingAcquisition(Lock lock)
		{
			LockAccessValidator.getInstance().attemptingAcquisition(AutonomousLock.this, getLevel(lock));
		}

		@Override
		public void lockAcquired(Lock lock)
		{
			LockAccessValidator.getInstance().lockAcquired(AutonomousLock.this, getLevel(lock));
		}

		@Override
		public void lockReleased(Lock lock)
		{
			LockAccessValidator.getInstance().lockReleased(AutonomousLock.this, getLevel(lock));
		}
	}

	private final HookedLock<ReentrantLock> assemblyLock;
	private final HookedReadWriteLock commitLock;

	AutonomousLock(String description)
	{
		super(description);

		assemblyLock = new HookedLock<ReentrantLock>(new ReentrantLock());
		commitLock = new HookedReadWriteLock();

		assemblyLock.setHook(new Hook());
		commitLock.setBothHooks(new Hook());
	}

	Level getLevel(Lock lock)
	{
		if (lock == assemblyLock)
		{
			return Level.ASSEMBLY;
		}
		else if (lock == commitLock.readLock())
		{
			return Level.READ;
		}
		else
		{
			return Level.COMMIT;
		}
	}

	boolean hasReadPermission()
	{
		return commitLock.writeLock().isHeldByCurrentThread() || (commitLock.getReadHoldCount() > 0);
	}

	boolean hasAssemblyPermission()
	{
		return assemblyLock.getLock().isHeldByCurrentThread();
	}

	boolean hasWritePermission()
	{
		return commitLock.writeLock().isHeldByCurrentThread();
	}

	Result readLock(RequestMode mode)
	{
		if (commitLock.getReadHoldCount() > 0)
		{
			return Result.SUCCESS;
		}

		if (mode == RequestMode.IMMEDIATE)
		{
			if (commitLock.readLock().tryLock())
			{
				return Result.SUCCESS;
			}
			else
			{
				return Result.BUSY;
			}
		}
		else
		{
			try
			{
				if (commitLock.readLock().tryLock(50, TimeUnit.MILLISECONDS))
				{
					return Result.SUCCESS;
				}
				else
				{
					return Result.TIMEOUT;
				}
			}
			catch (InterruptedException e)
			{
				return Result.INTERRUPTED;
			}
		}
	}

	void clearReadLocks()
	{
		while (commitLock.getReadHoldCount() > 0)
		{
			commitLock.readLock().unlock();
		}
	}

	Result assemblyLock(RequestMode mode)
	{
		if (assemblyLock.getLock().isHeldByCurrentThread())
		{
			System.err.println("Warning, duplicate assembly-lock attempt on " + this);
			return Result.SUCCESS;
		}

		if (mode == RequestMode.IMMEDIATE)
		{
			if (assemblyLock.tryLock())
			{
				return Result.SUCCESS;
			}
			else
			{
				return Result.BUSY;
			}
		}
		else
		{
			try
			{
				if (assemblyLock.tryLock(50, TimeUnit.MILLISECONDS))
				{
					return Result.SUCCESS;
				}
				else
				{
					return Result.TIMEOUT;
				}
			}
			catch (InterruptedException e)
			{
				return Result.INTERRUPTED;
			}
		}
	}

	Result commitLock(RequestMode mode)
	{
		if (!assemblyLock.getLock().isHeldByCurrentThread())
		{
			throw new IllegalStateException("Cannot acquire the commit lock without first holding the assembly lock: " + this);
		}

		if (commitLock.writeLock().isHeldByCurrentThread())
		{
			System.err.println("Warning, duplicate write-lock attempt on " + this);
			return Result.SUCCESS;
		}

		clearReadLocks();

		if (mode == RequestMode.IMMEDIATE)
		{
			if (commitLock.writeLock().tryLock())
			{
				return Result.SUCCESS;
			}
			else
			{
				return Result.BUSY;
			}
		}
		else
		{
			try
			{
				if (commitLock.writeLock().tryLock(50, TimeUnit.MILLISECONDS))
				{
					return Result.SUCCESS;
				}
				else
				{
					return Result.TIMEOUT;
				}
			}
			catch (InterruptedException e)
			{
				return Result.INTERRUPTED;
			}
		}
	}

	void commitUnlock()
	{
		if (!commitLock.writeLock().isHeldByCurrentThread())
		{
			System.err.println("Warning, duplicate write-unlock attempt on " + this);
			return;
		}

		assemblyLock.unlock();
		clearReadLocks();
		commitLock.writeLock().unlock();
	}

	void emergencyReleaseAllLock()
	{
		clearReadLocks();
		while (assemblyLock.getLock().isHeldByCurrentThread())
		{
			assemblyLock.unlock();
		}
		while (commitLock.writeLock().isHeldByCurrentThread())
		{
			commitLock.writeLock().unlock();
		}
	}

	@Override
	public AutonomousLock getAutonomousLock()
	{
		return this;
	}
}
