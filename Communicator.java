package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator
{
	private Lock lock;
	private Condition2 outQueue;
	private Condition2 synchQueue;
	private Condition2 inQueue;
	private boolean unreadData;
	private int savedDatum;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator()
	{
		lock = new Lock();
		outQueue = new Condition2(lock);
		synchQueue = new Condition2(lock);
		inQueue = new Condition2(lock);
		unreadData = false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param word the integer to transfer.
	 */
	public void speak(int word)
	{
		lock.acquire();

		while (unreadData)
		{
			outQueue.sleep(); // sleep if data is already present and waiting to be read
		}

		savedDatum = word; // save datum
		unreadData = true; // set data presence flag

		inQueue.wake(); // wake up a waiting listener

		synchQueue.sleep(); // wait specifically until OUR message is read by a listener

		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return the integer transferred.
	 */
	public int listen()
	{
		lock.acquire();

		while (!unreadData)
		{
			inQueue.sleep(); // sleep when no available data
		}

		int word = savedDatum; // load datum to be transferred
		unreadData = false; // mark data as read

		synchQueue.wake(); // wake the specific speaker whose message was just read
		outQueue.wake(); // wake the next waiting speaker in line to send its data

		lock.release();

		return word;
	}

	public static void selfTest()
	{
		System.out.println("\n--- Starting Communicator.selfTest() ---");

		// We want to test 2 things: a basic speaker-listener exchange, and multiple
		// speakers and listeners communicating at the same time.
		final Communicator comm = new Communicator();

		// Test 1: A single speaker sending a message to a listener
		System.out.println("--- Test 1: Basic communication ---");

		final int testMessage = 42;
		final int[] received = new int[1];

		KThread speaker = new KThread(new Runnable()
		{
			public void run()
			{
				System.out.println("Speaker: Sending message " + testMessage);
				comm.speak(testMessage);
				System.out.println("Speaker: Message received.");
			}
		}).setName("Speaker-1");

		KThread listener = new KThread(new Runnable()
		{
			public void run()
			{
				received[0] = comm.listen();
				System.out.println("Listener: Received message " + received[0]);
			}
		}).setName("Listener-1");

		speaker.fork();
		listener.fork();

		speaker.join();
		listener.join();

		Lib.assertTrue(received[0] == testMessage);

		System.out.println("Test 1 passed.");

		// Test 2: Multiple speakers and listeners exchanging messages at the same time
		System.out.println("--- Test 2: Multiple concurrent exchanges ---");

		final int numMessages = 6;

		final int[] messages =
		{ 100, -50, 0, 999, Integer.MIN_VALUE, Integer.MAX_VALUE };

		final int[] results = new int[numMessages];

		KThread[] speakers = new KThread[numMessages];
		KThread[] listeners = new KThread[numMessages];

		// create speaker threads
		for (int i = 0; i < numMessages; i++)
		{
			final int id = i;

			speakers[i] = new KThread(new Runnable()
			{
				public void run()
				{
					System.out.println("Speaker " + id + ": Sending " + messages[id]);
					comm.speak(messages[id]);
				}
			}).setName("Speaker-" + (i + 2));
		}

		// create listener threads
		for (int i = 0; i < numMessages; i++)
		{
			final int id = i;

			listeners[i] = new KThread(new Runnable()
			{
				public void run()
				{
					results[id] = comm.listen();
					System.out.println("Listener " + id + ": Received " + results[id]);
				}
			}).setName("Listener-" + (i + 2));
		}

		// start all threads
		for (int i = 0; i < numMessages; i++)
		{
			speakers[i].fork();
			listeners[i].fork();
		}

		// wait for all speakers and listeners to finish
		for (int i = 0; i < numMessages; i++)
		{
			speakers[i].join();
			listeners[i].join();
		}

		// verify every message was received exactly once
		boolean[] found = new boolean[numMessages];

		for (int i = 0; i < numMessages; i++)
		{
			boolean matched = false;

			for (int j = 0; j < numMessages; j++)
			{

				if (!found[j] && results[i] == messages[j])
				{
					found[j] = true;
					matched = true;
					break;
				}
			}

			Lib.assertTrue(matched);
		}

		for (int i = 0; i < numMessages; i++)
		{
			Lib.assertTrue(found[i]);
		}

		System.out.println("All messages were exchanged.");

		System.out.println("Test 2 passed.");

		System.out.println("--- Communicator.selfTest() Completed ---\n");
	}
}