package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;
import java.util.Comparator;

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
     * Create a new WaitingThread container.
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

  // ensures that waitingThreads is a minheap
  private class WaitingThreadComparator implements Comparator<WaitingThread>
  {
    public int compare(WaitingThread x, WaitingThread y)
    {
      if(x.getWakeUpTime() < y.getWakeUpTime())
      {
	return -1;
      }// if
      else if(x.getWakeUpTime() == y.getWakeUpTime())
      {
	return 0;
      }// if
      else
      {
	return 1;
      }// else
    }// compare
  }// WaitingThreadComparator

  // minheap of threads sorted by desired wake up time that have called Alarm.waitUntil()
  private PriorityQueue<WaitingThread> waitingThreads = new PriorityQueue<WaitingThread>(1, new WaitingThreadComparator());

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

      // repeatedly check first element of minheap for all threads ready to run again
      for(WaitingThread firstElement = waitingThreads.peek();
	  firstElement != null && firstElement.getWakeUpTime() <= Machine.timer().getTime();
	  firstElement = waitingThreads.peek())
      {
	// lock this area down to prevent race conditions in the heap
	boolean intStatus = Machine.interrupt().disable();
	
	waitingThreads.poll().getThread().ready();
	
	Machine.interrupt().restore(intStatus);
      }// for
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
    	
      long wakeTime = Machine.timer().getTime() + x;

      // make sure time hasn't already passed, if it has skip all this crap and return immediately
      if(wakeTime > Machine.timer().getTime())
      {
	// prevent race conditions on the heap and put calling thread to sleep
	boolean intStatus = Machine.interrupt().disable();
    	    	
	waitingThreads.offer(new WaitingThread(KThread.currentThread(), wakeTime));
		
    	KThread.sleep();

    	//And remember to re-enable interrupts once this block is finished
    	Machine.interrupt().restore(intStatus);
      }// if
    }
}
