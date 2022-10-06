package mains;
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

    // private static final String[] events = { LED_MATRIX_TICKER_WAIT,
    // LED_MATRIX_READY, LED_MATRIX_ERROR,
    // LED_MATRIX_TICKER_FINISHED };

    private static enum State {
        Initializing, Idle, Ticking
    };

    private boolean matrixReady = false;
    private boolean mqttReady = false;

    private State state = State.Initializing;

    private MQTTclient client;

    TickerMachine() {
        Scheduler scheduler = new Scheduler(this);
        ticker = new LEDMatrixTicker(scheduler);
        client = new MQTTclient("tcp://broker.hivemq.com:1883", "192.168.0.196", true, scheduler);
        scheduler.start();
    }

    @Override
    public int fire(String event, Scheduler scheduler) {
        if (event.startsWith(MQTTclient.MESSAGE)) {
            switch (state) {
                case Initializing:
                    return DISCARD_EVENT;

                case Idle:
                    String message = event.substring(MQTTclient.MESSAGE.length()); // Will this slice away one character
                                                                                   // too much? nope
                    ticker.StartWriting(message);
                    this.state = State.Ticking;
                    return EXECUTE_TRANSITION;

                case Ticking:
                    return DISCARD_EVENT;
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
                        state = State.Idle;
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
