import runtime.Scheduler;

public class Freepool {
    private int freepools;
    public final static String FREEPOOL = "*";
    public final static String MESSAGE = "MESSAGE";
    private Scheduler s;

    Freepool(int capacity, Scheduler scheduler) {
        this.freepools = capacity;
        this.s = scheduler;
    }

    boolean sendData(String data) {
        if (freepools > 0) {
            freepools--;
            s.addToQueueLast(MESSAGE + data);
            return true;
        }
        return false;
    }

    void receiveFreepool() {
        freepools++;
    }

    
}
