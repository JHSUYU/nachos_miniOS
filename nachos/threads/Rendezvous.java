package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    //Here we create a hashmap where the key is the tag and the value is the slot corresponding to the tag

    static final class Node {
        int item; //The value it carries
        boolean useable; // Whether it is currently available
        boolean hasData; //Whether the data is currently available here
        volatile KThread parked;

        List<KThread> waitingThreads;

        public Node(){
            useable = true;
            hasData = false;
            parked = null;
            waitingThreads = new ArrayList<>();
        }
    }
    private HashMap<Integer, Node> tag2Node;

    public Rendezvous () {
        tag2Node = new HashMap<Integer, Node>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        int v;
        int item = value;
        v = slotExchange(tag, item);
        return v;
    }

    private final int slotExchange(int tag, int item) {
        //tag
        Node slot = new Node();
        if(tag2Node.containsKey(tag)) slot  = tag2Node.get(tag);
        else tag2Node.put(tag,slot);

        KThread t = KThread.currentThread();
        int val = 0;
        while(true){
            slot = tag2Node.get(tag);
            System.out.println("---------------");
            System.out.println("Thread " + KThread.currentThread().getName() + " hasData " +  slot.hasData + " useable " + slot.useable );
            if (slot.hasData && slot.useable) { // A slot that is not null indicates that a thread is already waiting to swap data
                slot.useable = false;
                // Get the data to be exchanged
                val = slot.item;
                // Wait for the data that the thread needs, and assign your item to match so that the other thread can get it
                slot.item = item;
                tag2Node.put(tag,slot);
                // Get the waiting thread
                KThread w = slot.parked;
                if(w != null){
                    //Wake thread w
                    boolean status = Machine.interrupt().disable();
                    w.ready();
                    Machine.interrupt().restore(status);
                }
                return val; // Return the data, and the exchange is complete
            } else if(!slot.hasData){ // There is no other thread to occupy the slot yet
                slot.hasData = true;
                slot.item = item;
                slot.parked = t;
                tag2Node.put(tag, slot);
                boolean status = Machine.interrupt().disable();
                System.out.println("Thread " + KThread.currentThread().getName() + " is going to sleep");
                t.sleep();
                // After wake up
                System.out.println("Thread " + KThread.currentThread().getName() + " wakes up");
                Machine.interrupt().restore(status);
                slot = tag2Node.get(tag);
                val = slot.item;
                slot.item = 0;
                slot.parked = null;
                slot.hasData = false;
                slot.useable = true;
                tag2Node.put(tag, slot);
                // wake up them if there are threads in waiting threads.
                Iterator<KThread> iteratorForWaitingThreads = slot.waitingThreads.iterator();
                int times = 2; // one time for two threads
                while(iteratorForWaitingThreads.hasNext() && (times--)>0 ){
                    KThread thread = iteratorForWaitingThreads.next();
                    boolean subStatus = Machine.interrupt().disable();
                    thread.ready();
                    iteratorForWaitingThreads.remove();
                    Machine.interrupt().restore(subStatus);
                    tag2Node.put(tag, slot);
                }
                return val;
            } else{ // There is data but it is being used, waiting thread -> add to waitingThreads
                System.out.println("Thread " + KThread.currentThread().getName() + " wait");
                boolean status = Machine.interrupt().disable();
                slot.waitingThreads.add(t);
                tag2Node.put(tag,slot);
                t.sleep();
                Machine.interrupt().restore(status);
            }
        }
    }

    // Place Rendezvous test code inside of the Rendezvous class.
    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();
        System.out.println ( "Rendezvous rendezTest1\n" );
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -2;
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 2;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -2, "Was expecting " + -2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");
        KThread t5 = new KThread( new Runnable () {
            public void run() {
                int tag = 1;
                int send = -3;
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 3, "Was expecting " + 3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t5.setName("t5");
        KThread t6 = new KThread( new Runnable () {
            public void run() {
                int tag = 1;
                int send = 3;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -3, "Was expecting " + -3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t6.setName("t6");
        t1.fork();  t5.fork(); t2.fork(); t3.fork(); t4.fork();t6.fork();
        // assumes join is implemented correctly
        // Wait for t1 to finish before executing the next block
        t1.join(); t5.join(); t2.join(); t3.join(); t4.join();  t6.join();
    }


}
