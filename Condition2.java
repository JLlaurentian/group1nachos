package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupts for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2
{
	/**
	 * Allocate a new condition variable.
	 *
	 * @param conditionLock the lock associated with this condition variable. The
	 *                      current thread must hold this lock whenever it uses
	 *                      <tt>sleep()</tt>, <tt>wake()</tt>, or
	 *                      <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock)
	{
		this.conditionLock = conditionLock;
		this.waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep()
	{
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean oldStatus = Machine.interrupt().disable(); // disable interrupts

		waitQueue.add(KThread.currentThread());
		conditionLock.release(); // release lock

		KThread.sleep(); // put thread to sleep

		conditionLock.acquire(); // acquire lock
		Machine.interrupt().restore(oldStatus); // restore interrupts
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wake()
	{
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean oldStatus = Machine.interrupt().disable(); // disable interrupts

		if (!waitQueue.isEmpty())
		{
			KThread thread = waitQueue.removeFirst();
			thread.ready();
		}

		Machine.interrupt().restore(oldStatus); // restore interrupts
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current thread
	 * must hold the associated lock.
	 */
	public void wakeAll()
	{
		Lib.assertTrue(conditionLock.isHeldByCurrentThread()); // disable interrupts

		boolean oldStatus = Machine.interrupt().disable();

		while (!waitQueue.isEmpty())
		{
			KThread thread = waitQueue.removeFirst();
			thread.ready();
		}

		Machine.interrupt().restore(oldStatus); // restore interrupts
	}

	public static void selfTest()
	{
		System.out.println("\n--- Starting Condition2.selfTest() ---");

		// we want to test 2 things: a single thread using sleep() and wake(), and
		// multiple threads using sleep() and wakeAll().

		final Lock lock = new Lock();
		final Condition2 cv = new Condition2(lock);

		// Test 1: A single thread going to sleep and being woken up with wake()
		System.out.println("--- Test 1: Single thread sleep and wake() ---");

		final Semaphore ready1 = new Semaphore(0);

		KThread t1 = new KThread(new Runnable()
		{
			public void run()
			{
				lock.acquire();
				System.out.println("T1: Going to sleep.");
				ready1.V();
				cv.sleep();
				System.out.println("T1: Woke up.");
				lock.release();
			}
		});

		t1.fork();

		// wait until T1 is ready to sleep
		ready1.P();

		lock.acquire();
		System.out.println("Waking up T1.");
		cv.wake();
		lock.release();

		t1.join();

		System.out.println("Test 1 passed.");

		// Test 2: Multiple threads going to sleep and being woken up with wakeAll()
		System.out.println("--- Test 2: Multiple threads sleep and wakeAll() ---");

		final int numThreads = 3;
		final Semaphore ready2 = new Semaphore(0);
		KThread[] workers = new KThread[numThreads];

		for (int i = 0; i < numThreads; i++)
		{
			final int id = i;

			workers[i] = new KThread(new Runnable()
			{
				public void run()
				{
					lock.acquire();
					System.out.println("Worker " + id + ": Going to sleep.");
					ready2.V();
					cv.sleep();
					System.out.println("Worker " + id + ": Woke up.");
					lock.release();
				}
			});

			workers[i].fork();
		}

		// wait until all workers are ready to sleep
		for (int i = 0; i < numThreads; i++)
		{
			ready2.P();
		}

		lock.acquire();
		System.out.println("Waking all workers");
		cv.wakeAll();
		lock.release();

		// wait for all workers to finish
		for (int i = 0; i < numThreads; i++)
		{
			workers[i].join();
		}

		System.out.println("Test 2 passed.");

		System.out.println("--- Condition2.selfTest() Completed ---\n");
	}

	private Lock conditionLock;
	private LinkedList<KThread> waitQueue;
}