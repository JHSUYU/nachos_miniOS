package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	private List<KThread> waitingThreads;
	private List<Long> wakeTimes;

	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
		waitingThreads=new ArrayList<>();
		wakeTimes=new ArrayList<>();
	}



	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		long curTime=Machine.timer().getTime();
		int i=0;
		Iterator<KThread> iteratorForWaitngThreads=waitingThreads.iterator();
		Iterator<Long> iteratorForWakeTimes=wakeTimes.iterator();
		while(iteratorForWaitngThreads.hasNext()){
			Long wakeTimes= iteratorForWakeTimes.next();
			KThread thread=iteratorForWaitngThreads.next();
			if(wakeTimes<=curTime){
				thread.ready();
				iteratorForWaitngThreads.remove();
				iteratorForWakeTimes.remove();
			}
		}
//		System.out.println("Before time interrupt yield "+KThread.currentThread().getName());
		KThread.currentThread().yield();
//		System.out.println("After time interrupt yield "+KThread.currentThread().getName());
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 *
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 *
	 * @param x the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		if(x<=0){
			return;
		}
		long wakeTime = Machine.timer().getTime() + x;
//		System.out.println("Wait Until Current Thread"+KThread.currentThread().getName());
		waitingThreads.add(KThread.currentThread());
		wakeTimes.add(wakeTime);
//		System.out.println("Current Wake Time is"+wakeTime);
//		while(wakeTime<Machine.timer().getTime()){
//			KThread.waitUntilYield();
//		}
		KThread.waitUntilYield();
	}

	/**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 *
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
	public boolean cancel(KThread thread) {
		return false;
	}

	// Add Alarm testing code to the Alarm class

	public static void alarmTest1() {
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;


		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Implement more test methods here ...

	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest() {
		alarmTest1();
		// Invoke your other test methods here ...
	}
}
