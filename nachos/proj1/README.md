### Proj1 ReadMe


#### Group Member: Zhenyu Li, Zhengyang Li, Xingfei Xu

#### 1.What code we wrote and how each group member contributed to the project.

We finished the assignments and extra work of Proj1.

- Zhenyu finished Alarm.waitUntil() In waitUntil(), he used the queue to store the thread that calls waitUntil and its waiting time. Timer.interrupt() routine will check whether the waiting

  threads in  the queue has waited for enough time.Assuming the time when the thread waits is x1, and we let the thread wait d ticks. In timer.interrupt() routine, if current time x2>=x1+d, then the thread would be woken via ready() routine, and once they are woken, the threads will be removed from the waiting queue.

- Zhenyu finished KThread.join(), he firstly used hashset in Java to store the thread who has called join(). If the same thread calls join() again, the program will find the hashset contains the thread, and it will fail(Nachos asserts). Besides, he used a hashtable to store the entry<called thread, calling thread>. The KThread.finish() routine will change the called thread status to finished, at this time, the called thread will get calling thread via hashtable.get() and wake the calling thread. After that, the entry in hashtable will be removed.

- Xingfei finished Rendezvous class, using a slot stuct for exchange for each tag and using a hashmap to store tags and its corresponding nodes. When slot is empty, then set slot with value and make the current thread sleep. When the next thread comes in and find the slot isn't empty, it will wake the thread in the slot up and exchange the values.  

- Xingfei finished Future class. Because the nachos has one CPU, the asynchronous operation cannot be totally in parallel process. So he simulated the asynchronous operations by working on those tasks one second then switch to the next task. In this way, the process seems like working in parallel process, which means working asynchronous. Use consumeTime function to represent the time consuming operations for the process.

- Zhengyang finished Condition2 class, implementing the methods of `sleep()`, `wake()`, `wakeAll()` and `sleepFor()`.

  - First, he created the object of the PriorityQueen class of PriorityScheduler class, defined the waitQueue to store the thread by priority and implemented the methods in PriorityQueen class, like `waitForAccess()` , `acquire()`  , `nextThread()` and `pickNextThread()` for the better decoupling and scalability.
  - In `sleep()` method, he disabled the interrupt and put the current thread into the queue waiting for wake-up, so there is no chance the sleeper will miss the wake-up, even though the lock is released before calling `sleep()` in KThread.
  - In `wake()` and `wakeAll()` method,  he invoked `pickNextThread()` to check availability of the next thread, and got the next thread using `nextThread()` and moved this thread to the ready state by calling  `ready()`
  - For `sleepFor()`, using the queue to store the current thread incorporated with the waitUntil function written by Zhenyu, One thread could wake up by another thread invoking `wake()`, or its timeout elapses.




#### 2.How did we test 

##### WaitUntil

- Zhenyu tested the program will work when 1)the parameter of waitUntil is less than zero 2)multiple threads call waitUntil and they will wake at proper time.

##### Join

- Zhenyu tested the program will work when 1) Let us say thread d calls thread c.join(), thread c call thread b.join() and thread b call thread a.join(), then the threads should execute in the sequence of 

  a->b->c->d 2) when thread a calls a.join() if a has finished, then nothing will happen 3)if the thread calls join() more than once, nachos asserts. 4) a.start(),a.join() b.start,b.join() or a.start(),b.start(), a.join(),b.join() then the threads should execute in proper order.

**Condition2**

- Zhengyang tested the Condition2 class by validating whether the two threads strictly alternate their execution with each other using a condition variable. Since the Condition2 class is equivalent to the Condition class, so both of them should have exactly same behavior. By testing Condition class first, then check the behavior of Condition2 class.

**SleepFor**

- Zhengyang tested the sleepFor first using the timeout without another thread wake up it, so the thread will wake up when the timeout expires. And by setting different timeouts for different threads, he used several threads called part of the threads wake up, and the sequence strictly obeyed the position in waitQueue or the individuals' timeout elapsed. 

##### Rendezvous

- Xingfei tested the Rendezvous class by using 6 threads which in 3 diffrent tags. For example, thread 1 and 2 are in tag 1, thread 3 and 4 are in tag 2, thread 5 and 6 are in tag 3. And run each thread to see the result. It works no matter which order of these threads are called. 

##### Future

- Xingfei tested the Future class by using two different tasks. One task need to be done using 5 seconds and the other need 1 seconds. Frist, he called the first task (which needs 5 seconds to fininsh). And immediately, he called the second task (which need 1 second to finish). Then, use consumeTime function to consume time by 3 seconds. Then, get result of task1 and task2, which shows task1 hasn't been finished and task2 has already been finished. Then, use consumeTime function to consume time by 3 seconds, the results shows both of them were finished.

