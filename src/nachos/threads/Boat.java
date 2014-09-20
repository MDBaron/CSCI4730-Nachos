package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
  static int childrenOnOahu;
  static int childrenOnMolokai = 0;
  static int adultsOnOahu;
  static int childrenInBoat = 0;
  static boolean boatOnOahu = true;
  static boolean adultTurn = false;
  static Lock lock = new Lock();
  static Condition adult = new Condition(lock);
  static Condition childOnOahu = new Condition(lock);
  static Condition childOnMolokai = new Condition(lock);
  static Condition boatFull = new Condition(lock);
  static Condition boatGo = new Condition(lock);
  static Condition boatEmpty = new Condition(lock);
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//	begin(1, 2, b);

	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
	begin(3, 3, b);

//	System.out.println("\n ***Testing Boats with 4 children, 0 adults***");
//	begin(0, 4, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	childrenOnOahu = children;
	adultsOnOahu = adults;

	class Adult implements Runnable
	{
	  public void run()
	  {
	    Boat.AdultItinerary();
	  }
	};

	class Child implements Runnable
	{
	  public void run()
	  {
	    Boat.ChildItinerary();
	  }
	};
  
	// create threads
	KThread adultThreads[] = new KThread[adults];
	for(int i = 0; i < adults; i++)
	{
	  adultThreads[i] = new KThread(new Adult());
	  adultThreads[i].setName("Adult " + i);
	  adultThreads[i].fork();
	}// for

	KThread childrenThreads[] = new KThread[children];
	for(int i = 0; i < children; i++)
	{
	  childrenThreads[i] = new KThread(new Child());
	  childrenThreads[i].setName("Child " + i);
	  childrenThreads[i].fork();
	}// for

	// wait for all threads to finish
	for(int i = 0; i < adults; i++)
	{
	  adultThreads[i].join();
	}// for
	for(int i = 0; i < children; i++)
	{
	  childrenThreads[i].join();
	}// for
	
	// yay!
	System.out.println("Everybody safely made it to Molokai.");
    }

    static void AdultItinerary()
    {
      lock.acquire();
      
      // wait until signalled to go
      adult.sleep();

      // when signalled, one adult will row over
      bg.AdultRowToMolokai();
      adultsOnOahu--;
      adultTurn = false;
      boatOnOahu = false;

      // tell the child on molokai to row back to oahu
      childOnMolokai.wake();

      lock.release();
      // once the adult is on molokai, he can go off to play and let the children finish working
    }

    static void ChildItinerary()
    {
      boolean rower = false;

      lock.acquire();

      while(true)
      {
	// if the boat is on molokai, the boat is full, or it's the adult's turn,
	// all children must wait
	while(!boatOnOahu || childrenInBoat >= 2 || adultTurn)
	{
	  childOnOahu.sleep();
	}// while
	
	// first child gets into the boat
	if(childrenInBoat == 0)
	{
	  // take the helm
	  rower = true;
	  childrenInBoat++;
	  boatFull.sleep();// wait for passenger
	  
	  // when the passenger is ready we set sail
	  bg.ChildRowToMolokai();
	  boatGo.wake();// tell passenger we're shipping off 
	  boatOnOahu = false;
	}// if
	else
	{
	  // get in boat
	  childrenInBoat++;
	  boatFull.wake();// tell rower you're ready
	  boatGo.sleep();// wait for rower to ship off
	  
	  // head out with the rower
	  bg.ChildRideToMolokai();
	}// else
	
	// sail across the beautiful Pacific
	childrenOnOahu--;
	
	// rower lands on molokai
	if(rower)
	{
	  // get out of the boat
	  rower = false;
	  childrenInBoat--;

	  // make sure passenger is out of the boat before continuing
	  if(childrenInBoat > 0)
	  {
	    boatEmpty.sleep();
	  }// if
	  else 
	  {
	    boatEmpty.wake();
	  }// else
	  
	  // if no one is left on oahu, then we're done
	  if(childrenOnOahu == 0 && adultsOnOahu == 0)
	  {
	    // tell any children waiting on Molokai that they're free to go
	    childOnMolokai.wakeAll();
	    
	    // then go play (thread dies)
	    break; // out of infinite loop
	  }// if
	  else // if someone is still left on oahu, we have to go back and get them
	  {
	    // row over
	    bg.ChildRowToOahu();
	    
	    // land back on oahu
	    childrenOnOahu++;
	    boatOnOahu = true;
	    
	    // if there's an adult, let him ride over
	    if(adultsOnOahu > 0 && childrenOnMolokai > 0)
	    {
	      adultTurn = true;
	      adult.wake();
	    }// if
	    else // ride over with another child
	    {
	      childOnOahu.wake();
	    }// else
	  }// else
	}// if
	else // passenger lands on molokai
	{
	  // get out of boat
	  childrenInBoat--;

	  // make sure rower is out of the boat before continuing
	  if(childrenInBoat > 0)
	  {
	    boatEmpty.sleep();
	  }// if
	  else 
	  {
	    boatEmpty.wake();
	  }// else
	  
	  // if no one is left on oahu, then we're done
	  if(childrenOnOahu == 0 && adultsOnOahu == 0)
	  {
	    // go play (thread dies)
	    break; // out of infinite loop
	  }// if
	  else // people are still on oahu
	  {
	    // wait until someone else arrives
	    childrenOnMolokai++;
	    childOnMolokai.sleep();
	    
	    // when woken up, check the mission status
	    // if no one is left on oahu, then we're done
	    if(childrenOnOahu == 0 && adultsOnOahu == 0)
	    {
	      // go play (thread dies)
	      break; // out of infinite loop
	    }// if
	    else // if someone is still left on oahu, we have to go back and get them
	    {
	      // row over
	      childrenOnMolokai--;
	      bg.ChildRowToOahu();
	      
	      // land back on oahu
	      childrenOnOahu++;
	      boatOnOahu = true;
	      
	      // if there's an adult, let him ride over
	      if(adultsOnOahu > 0 && childrenOnMolokai > 0)
	      {
		adultTurn = true;
		adult.wake();
	      }// if
	      else // ride over with another child
	      {
		childOnOahu.wake();
	      }// else
	    }// else
	  }// else
	}// else
      }// while

      lock.release();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
