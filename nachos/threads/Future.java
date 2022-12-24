package nachos.threads;

import java.util.*;
import java.util.function.IntSupplier;
import nachos.machine.*;

/**
 * A <i>Future</i> is a convenient mechanism for using asynchonous
 * operations.
 */
public class Future {

    private int data = 0;
    private boolean completed = false;
    int currentTime = 10000;
    private KThread thread = null;
    public void makeRealData(){
        if (currentTime > 0) return;
        this.data = 10;
        this.completed = true;
        return;
    }

    /**
     * Instantiate a new <i>Future</i>.  The <i>Future</i> will invoke
     * the supplied <i>function</i> asynchronously in a KThread.  In
     * particular, the constructor should not block as a consequence
     * of invoking <i>function</i>.
     */
    public boolean isCompleted(){
        return this.completed;
    }
    public Future (int time) {
        thread = new KThread(new Runnable (){
            public void run(){
                makeRealData();
                currentTime = time;
            }
        });
        thread.setName("thread1");
        thread.fork();
        thread.join();
    }

    /**
     * Return the result of invoking the <i>function</i> passed in to
     * the <i>Future</i> when it was created.  If the function has not
     * completed when <i>get</i> is invoked, then the caller is
     * blocked.  If the function has completed, then <i>get</i>
     * returns the result of the function.  Note that <i>get</i> may
     * be called any number of times (potentially by multiple
     * threads), and it should always return the same value.
     */

    public int get(){
        if(!this.completed) return -1;
        else return this.data;
    }

    public void consumeTime(int time){
        currentTime -= time;
        makeRealData();
    }


    public static void futureTest() {
        final Future f1 = new Future(5);
        final Future f2 = new Future(1);
        System.out.println("first trail");
        System.out.println("f1 value: " + f1.get());
        System.out.println("f2 value: " + f2.get());
        f1.consumeTime(3);
        f2.consumeTime(3);
        System.out.println("second trail");
        System.out.println("f1 value: " + f1.get());
        System.out.println("f2 value: " + f2.get());
        f1.consumeTime(3);
        f2.consumeTime(3);
        System.out.println("third trail");
        System.out.println("f1 value: " + f1.get());
        System.out.println("f2 value: " + f2.get());
        return;
    }
}
