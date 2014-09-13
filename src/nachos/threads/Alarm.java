package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

  // Entity class that represents a KThread that is waiting until a certain
  // time to wake up
  private class WaitingThread
  {
    KThread thread;
    long wakeUpTime;

    /**
     * Create a new waiting thread container.
     *
     * @param thread the thread that is waiting
     * @param wakeUpTime the system time at which the thread should wake
     *
     */
    public WaitingThread(KThread thread, long wakeUpTime)
    {
      super();
      this.thread = thread;
      this.wakeUpTime = wakeUpTime;
    }// ctor

    public KThread getThread()
    {
      return this.thread;
    }// getThread

    public long getWakeUpTime()
    {
      return this.wakeUpTime;
    }// getWakeUpTime
  }// WaitingThread

  // list of threads that have called Alarm.waitUntil()
  private ArrayList<WaitingThread> waitingThreads = new ArrayList<WaitingThread>();

    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	/*
    	 * So far this only yields the thread. 
    	 */
    	//TO-DO: Check for time delta to add sleeping thread to readyQueue. 
    	//TO-DO: if(elapsedTime >= necessarySleepTime){ pop thread from sleepingList? }
    	//TO-DO: This will need locks, due to multiple threads accessing

      // iterate through list of waiting threads and wake the appropriate ones
      for(int i = 0; i < waitingThreads.size(); i++)
      {
	if(waitingThreads.get(i).getWakeUpTime() <= Machine.timer().getTime())
	{
	  // lock this area down to prevent race conditions in the ArrayList
	  boolean intStatus = Machine.interrupt().disable();

	  waitingThreads.remove(i).getThread().ready();

	  Machine.interrupt().restore(intStatus);
	}// if
      }// for
    	
      // KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    	
    	/* -Matty
    	 * This needs to be refactored:
    	 * Suspend caller thread until the current time + x has elapsed.
    	 * Then shift to the ready queue of the timer interrupt handler(this must be altered as well) 
    	 * 
    	 */
    	
    	//Re-use timer call from old method? Not sure how else to get the system time
      long wakeTime = Machine.timer().getTime() + x;

      // make sure time hasn't already passed, if it has skip all this crap and return immediately
      if(wakeTime > Machine.timer().getTime())
      {
	
    	//TO-DO: "All thread queue methods must be invoked with <b>interrupts disabled</b>." <-- In ThreadQueue abstract class, 
    	//so we should probably disable interrupts here in order to add the caller thread to the ThreadQueue readyQueue
    	//I would think this to be the case if there is only one alarm, and any number of threads can access it at a time.
    	
	// prevent race conditions on the ArrayList
	boolean intStatus = Machine.interrupt().disable();
    	
    	//TO-DO: Central block for Alarm to suspend the thread and toss it onto a queue
    	//Looks like ready queue should be accessed from timerInterrupt() 
    	//Maybe add it to a "sleeping" list/queue so timerInterrupt() can check it as timer() calls it?
    	//Queue would alright if they were chronologically sorted, but a list structure could be random and still function
    	
	waitingThreads.add(new WaitingThread(KThread.currentThread(), wakeTime));
	
	//After it's on the queue safely, and its time delta is being tracked we can suspend the thread
    	KThread.sleep();

    	//And remember to re-enable interrupts once this block is finished
    	Machine.interrupt().restore(intStatus);
      }// if
      

    	
    	/*  OLD THINGS AND DRAGONS  */
    		// for now, cheat just to get something working (busy waiting is bad)
    		//long wakeTime = Machine.timer().getTime() + x;
    		//while (wakeTime > Machine.timer().getTime())
    		// KThread.yield();
    	/*  OK, DRAGONS ARE GONE  */
    }

  
}
