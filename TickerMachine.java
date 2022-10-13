
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import mqtt.MQTTclient;
import runtime.IStateMachine;
import runtime.Scheduler;
import runtime.Timer;
import sensehat.LEDMatrixTicker;

public class TickerMachine implements IStateMachine {

    private LEDMatrixTicker ticker;
    private static final String TIMER = "TIMER";
    private Timer timer = new Timer(TIMER);

    private static final String LED_MATRIX_TICKER_WAIT = "LEDMatrixTickerWait",
            LED_MATRIX_READY = "LEDMatrixReady",
            LED_MATRIX_ERROR = "LEDMatrixError",
            LED_MATRIX_TICKER_FINISHED = "LEDMatrixTickerFinished";

    private static enum State {
        Initializing, Idle, Ticking
    };
    private State state = State.Initializing;

    private boolean matrixReady = false;
    private boolean mqttReady = false;

    private MQTTclient client;

    private BlockingDeque<String> queue = new LinkedBlockingDeque<String>();

    public static final String TOPIC = "ttm4160-team1-ticker";

    TickerMachine() {
        Scheduler scheduler = new Scheduler(this);
        ticker = new LEDMatrixTicker(scheduler);
        client = new MQTTclient("tcp://broker.hivemq.com:1883", "ttm4160-team1-ticker", true, scheduler);
        scheduler.start();
    }

    @Override
    public int fire(String event, Scheduler scheduler) {
        if (event.startsWith(Freepool.MESSAGE)) {
            String message = event.substring(Freepool.MESSAGE.length());
            switch (state) {
                case Initializing:
                    return DISCARD_EVENT;

                case Ticking:
                    queue.addLast(message);
                    return EXECUTE_TRANSITION;

                case Idle:
                    if (queue.isEmpty()) {
                        ticker.StartWriting(message);
                    } else {
                        queue.add(message);
                        try {
                            ticker.StartWriting(queue.take());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    this.state = State.Ticking;
                    return EXECUTE_TRANSITION;
            }
        }

        switch (event) {
            case LED_MATRIX_READY:
                switch (state) {
                    case Initializing:
                        if (mqttReady) {
                            state = State.Idle;
                        } else {
                            matrixReady = true;
                        }
                        return EXECUTE_TRANSITION;

                    default:
                        return DISCARD_EVENT;
                }
                
            case MQTTclient.READY:
                switch (state) {
                    case Initializing:
                        client.subscribeToTopic(InputMachine.TOPIC);
                        if (matrixReady) {
                            state = State.Idle;
                        } else {
                            mqttReady = true;
                        }
                        return EXECUTE_TRANSITION;
                    default:
                        return DISCARD_EVENT;
                }

            case LED_MATRIX_ERROR:
                return TERMINATE_SYSTEM;

            case LED_MATRIX_TICKER_WAIT:
                switch (state) {
                    case Ticking:
                        timer.start(scheduler, 100);
                        return EXECUTE_TRANSITION;
                    default:
                        return DISCARD_EVENT;
                }

            case LED_MATRIX_TICKER_FINISHED:
                switch (state) {
                    case Ticking:
                        client.sendMessage(TickerMachine.TOPIC, Freepool.FREEPOOL);
                        if (queue.isEmpty()) {
                            state = State.Idle;
                        }
                        else {
                            try {
                                ticker.StartWriting(queue.take());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        return EXECUTE_TRANSITION;
                    default:
                        return DISCARD_EVENT;
                }

            case TIMER:
                switch (state) {
                    case Ticking:
                        ticker.WritingStep();
                        return EXECUTE_TRANSITION;
                    default:
                        return DISCARD_EVENT;
                }

            case MQTTclient.ERROR:
                return TERMINATE_SYSTEM;

            default:
                return DISCARD_EVENT;
        }
    }

    public static void main(String[] args) {
        new TickerMachine();
    }
}
