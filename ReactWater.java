
package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

public class ReactWater

{
	/**
	 * Constructor of ReactWater
	 */

	public ReactWater()
	{

		lock = new Lock();

		hydrogenCV = new Condition2(lock);
		oxygenCV = new Condition2(lock);

		waitingHydrogen = new LinkedList<KThread>();
		waitingOxygen = new LinkedList<KThread>();

	}

	/**
	 * When H element comes, if there already exist another H element and an O
	 * element, then call the method of Makewater(). Or let H element wait in line.
	 */

	public void hReady()
	{
		lock.acquire();

		if (!waitingHydrogen.isEmpty() && !waitingOxygen.isEmpty())// check for waiting oxygen

		{
			// reserve elements
			waitingHydrogen.removeFirst();
			waitingOxygen.removeFirst();

			// wake waiting elements
			hydrogenCV.wake();
			oxygenCV.wake();

			makeWater();

			lock.release();

			return;

		}

		waitingHydrogen.addLast(KThread.currentThread());

		while (waitingHydrogen.contains(KThread.currentThread()))

		{
			hydrogenCV.sleep();
		}

		lock.release();

	}

	/**
	 * When O element comes, if there already exist another two H elements, then
	 * call the method of Makewater(). Or let O element wait in line.
	 */

	public void oReady()
	{
		lock.acquire();

		if (waitingHydrogen.size() >= 2) // check for 2 waiting hydrogen

		{
			// reserve 2 hydrogens
			waitingHydrogen.removeFirst();
			waitingHydrogen.removeFirst();

			// wake waiting hydrogens
			hydrogenCV.wake();
			hydrogenCV.wake();

			makeWater();

			lock.release();

			return;

		}

		waitingOxygen.addLast(KThread.currentThread());

		while (waitingOxygen.contains(KThread.currentThread()))
		{
			oxygenCV.sleep();
		}

		lock.release();

	}

	public static void makeWater()
	{
		System.out.println("Water was made!");
	}

	public static void selfTest()
	{
		System.out.println("\n--- Starting ReactWater.selfTest() ---");

		// We want to test 3 things: forming a molecule with H, H, O arrival order,
		// forming one with O, H, H arrival order, and forming multiple molecules with
		// mixed arrival order.

		final ReactWater rw = new ReactWater();

		// Test 1: Two hydrogen atoms arriving before one oxygen atom
		System.out.println("--- Test 1: H, H, O ---");

		KThread h1 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.hReady();
			}
		}).setName("H1");

		KThread h2 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.hReady();
			}
		}).setName("H2");

		KThread o1 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.oReady();
			}
		}).setName("O1");

		h1.fork();
		h2.fork();
		o1.fork();

		h1.join();
		h2.join();
		o1.join();

		System.out.println("Test 1 passed.");

		// Test 2: One oxygen atom arriving before two hydrogen atoms
		System.out.println("--- Test 2: O, H, H ---");

		KThread o2 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.oReady();
			}
		}).setName("O2");

		KThread h3 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.hReady();
			}
		}).setName("H3");

		KThread h4 = new KThread(new Runnable()
		{
			public void run()
			{
				rw.hReady();
			}
		}).setName("H4");

		o2.fork();
		h3.fork();
		h4.fork();

		o2.join();
		h3.join();
		h4.join();

		System.out.println("Test 2 passed.");

		// Test 3: Multiple molecules forming with hydrogen and oxygen arriving in
		// different orders
		System.out.println("--- Test 3: Multiple molecules ---");

		final int numHydrogen = 6;
		final int numOxygen = 3;

		KThread[] hydrogen = new KThread[numHydrogen];
		KThread[] oxygen = new KThread[numOxygen];

		for (int i = 0; i < numHydrogen; i++)
		{
			hydrogen[i] = new KThread(new Runnable()
			{
				public void run()
				{
					rw.hReady();
				}
			}).setName("H" + (i + 5));
		}

		for (int i = 0; i < numOxygen; i++)
		{
			oxygen[i] = new KThread(new Runnable()
			{
				public void run()
				{
					rw.oReady();
				}
			}).setName("O" + (i + 3));
		}

		// interleave hydrogen and oxygen arrivals.
		hydrogen[0].fork();
		oxygen[0].fork();
		hydrogen[1].fork();
		hydrogen[2].fork();
		oxygen[1].fork();
		hydrogen[3].fork();
		oxygen[2].fork();
		hydrogen[4].fork();
		hydrogen[5].fork();

		// wait for every thread to finish.
		for (int i = 0; i < numHydrogen; i++)
		{
			hydrogen[i].join();
		}

		for (int i = 0; i < numOxygen; i++)
		{
			oxygen[i].join();
		}

		System.out.println("Test 3 passed.");

		System.out.println("--- ReactWater.selfTest() Completed ---\n");
	}

	private Lock lock;

	private Condition2 hydrogenCV;

	private Condition2 oxygenCV;

	private LinkedList<KThread> waitingHydrogen;

	private LinkedList<KThread> waitingOxygen;

} // end of class ReactWater
