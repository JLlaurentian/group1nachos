package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

public class Alarm
{
	private LinkedList waitList = new LinkedList(); // list of waiting threads

	public Alarm()
	{
		Machine.timer().setInterruptHandler(new Runnable()
		{
			public void run()
			{
				timerInterrupt();
			}
		});
	}

	public void waitUntil(long x)
	{

		if (x <= 0) // skip waiting if time <= 0
		{
			return;
		}

		long wakeTime = Machine.timer().getTime() + x; // calculate when this thread should wake up

		boolean intStatus = Machine.interrupt().disable(); // disable interrupts

		WaitingThread wt = new WaitingThread(KThread.currentThread(), wakeTime);

		int i = 0;

		while (i < waitList.size() && ((WaitingThread) waitList.get(i)).wakeTime <= wakeTime)
		{
			i++;
		}
		waitList.add(i, wt);

		KThread.sleep(); // put thread to sleep

		Machine.interrupt().restore(intStatus); // restore interrupts
	}

	public void timerInterrupt()
	{
		boolean intStatus = Machine.interrupt().disable(); // disable interrupts

		long currentTime = Machine.timer().getTime();

		while (!waitList.isEmpty()) // pop threads until one is not ready
		{
			WaitingThread wt = (WaitingThread) waitList.getFirst();

			if (wt.wakeTime <= currentTime)
			{
				waitList.removeFirst();
				wt.thread.ready();
			} else
			{
				break;
			}
		}

		Machine.interrupt().restore(intStatus); // restore interrupts
	}

	public static void selfTest()
	{
		System.out.println("\n--- Starting Alarm.selfTest() ---");

		// we want to test 4 things: basic timed waits, wake-up ordering, zero and
		// negative waits, and multiple threads waiting at the same time.
		final Alarm alarm = new Alarm();

		// Test 1: A thread waiting for a specified amount of time
		System.out.println("--- Test 1: Basic waitUntil() ---");

		final long startTime = Machine.timer().getTime();

		KThread t1 = new KThread(new Runnable()
		{
			public void run()
			{
				alarm.waitUntil(100);
				long elapsed = Machine.timer().getTime() - startTime;

				System.out.println("Thread 1 woke up after " + elapsed + " ticks.");
				Lib.assertTrue(elapsed >= 100);
			}
		}).setName("Alarm-T1");

		t1.fork();
		t1.join();

		System.out.println("Test 1 passed.");

		// Test 2: Threads with different wait times waking up in the correct order
		System.out.println("--- Test 2: Wake-up ordering ---");

		final int[] wakeOrder = new int[3];
		final int[] index = new int[1];

		KThread slow = new KThread(new Runnable()
		{
			public void run()
			{
				alarm.waitUntil(300);
				wakeOrder[index[0]++] = 3;
			}
		}).setName("Alarm-Slow");

		KThread fast = new KThread(new Runnable()
		{
			public void run()
			{
				alarm.waitUntil(100);
				wakeOrder[index[0]++] = 1;
			}
		}).setName("Alarm-Fast");

		KThread medium = new KThread(new Runnable()
		{
			public void run()
			{
				alarm.waitUntil(200);
				wakeOrder[index[0]++] = 2;
			}
		}).setName("Alarm-Medium");

		// start the longest wait first.
		slow.fork();
		fast.fork();
		medium.fork();

		slow.join();
		fast.join();
		medium.join();

		Lib.assertTrue(wakeOrder[0] == 1);
		Lib.assertTrue(wakeOrder[1] == 2);
		Lib.assertTrue(wakeOrder[2] == 3);

		System.out.println("Threads woke up in the correct order.");

		System.out.println("Test 2 passed.");

		// Test 3: Zero and negative wait times returning immediately
		System.out.println("--- Test 3: Zero and negative waits ---");

		KThread immediate = new KThread(new Runnable()
		{
			public void run()
			{
				long before = Machine.timer().getTime();

				alarm.waitUntil(0);
				alarm.waitUntil(-100);

				long elapsed = Machine.timer().getTime() - before;

				Lib.assertTrue(elapsed < 100);
				System.out.println("Zero and negative waits returned immediately.");
			}
		}).setName("Alarm-Immediate");

		immediate.fork();
		immediate.join();

		System.out.println("Test 3 passed.");

		// Test 4: Multiple threads waiting at the same time
		System.out.println("--- Test 4: Concurrent waits ---");

		final int numThreads = 5;
		final int[] completed = new int[1];

		KThread[] workers = new KThread[numThreads];

		for (int i = 0; i < numThreads; i++)
		{
			final int id = i;

			workers[i] = new KThread(new Runnable()
			{
				public void run()
				{
					alarm.waitUntil(200);
					completed[0]++;

					System.out.println("Concurrent worker " + id + " woke up.");
				}
			}).setName("Alarm-Worker-" + i);

			workers[i].fork();
		}

		for (int i = 0; i < numThreads; i++)
		{
			workers[i].join();
		}

		Lib.assertTrue(completed[0] == numThreads);

		System.out.println("All concurrent workers finished.");

		System.out.println("Test 4 passed.");

		System.out.println("--- Alarm.selfTest() Completed ---\n");
	}

	// holds a thread and the time it should wake up
	private class WaitingThread
	{
		KThread thread;
		long wakeTime;

		WaitingThread(KThread t, long time)
		{
			thread = t;
			wakeTime = time;
		}
	}
}