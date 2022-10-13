import java.util.LinkedList;
import java.util.Queue;

import runtime.Scheduler;

public class Freepool {
    private int freepools;
    public final static String FREEPOOL = "*";
    public final static String MESSAGE = "MESSAGE";
    private Queue<String> queue = new LinkedList<>();
    private Scheduler s;

    Freepool(int capacity, Scheduler scheduler) {
        this.freepools = capacity;
        this.s = scheduler;
    }

    void sendData(String data) {
        if (freepools > 0) {
            freepools--;
            s.addToQueueLast(MESSAGE + data);
        } else {
            queue.add(data);
        }
    }

    void receiveFreepool() {
        freepools++;
        if (queue.size() > 0) {
            String element = queue.remove();
            sendData(element);
        }
    }

    
}
