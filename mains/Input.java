package mains;
import java.util.Scanner;

import runtime.Scheduler;

public class Input extends Thread {
    public static final String INPUT = "INPUT";
    private Scheduler scheduler;

    Input(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.start();
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);
        while (true) {
            scheduler.addToQueueLast(INPUT + in.next());
        }
    }
}
