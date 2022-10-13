
import mqtt.MQTTclient;

import runtime.IStateMachine;
import runtime.Scheduler;

public class InputMachine implements IStateMachine {
    public static final String TOPIC = "ttm4160-team1-input";

    /**
     * State: Initializing - setting up the MQTTClient
     * State: Active - MQTTClient ready to receive messages
     */
    private static enum State {
        Initializing, Active
    };

    private State state = State.Initializing;

    private Scheduler scheduler;
    private MQTTclient client;

    private Freepool freepool;

    InputMachine() {
        scheduler = new Scheduler(this);
        client = new MQTTclient("tcp://broker.hivemq.com:1883", "ttm4160-team1-inputmachine", true, scheduler);
        freepool = new Freepool(8, scheduler);
        new Input(scheduler);
        scheduler.start();
    }

    @Override
    public int fire(String event, Scheduler scheduler) {
        if (event.startsWith(Input.INPUT)) {
            switch (state) {
                case Active:
                    String message = event.substring(Input.INPUT.length());
                    if (freepool.sendData(message)) {
                        this.state = State.Active;
                        return EXECUTE_TRANSITION;
                    } else {
                        return DISCARD_EVENT;
                    }
                    
                default:
                    return DISCARD_EVENT;
            }
        }

        if (event.startsWith(Freepool.MESSAGE)) {
            switch (state) {
                case Initializing:
                    return DISCARD_EVENT;

                case Active:
                    client.sendMessage(TOPIC, event);
                    return EXECUTE_TRANSITION;
            }
        }

        switch (event) {
            case MQTTclient.READY:
                switch (state) {
                    case Initializing:
                        client.subscribeToTopic(TickerMachine.TOPIC);
                        state = State.Active;
                        return EXECUTE_TRANSITION;
                    default:
                        return DISCARD_EVENT;
                }

            case MQTTclient.ERROR:
                return TERMINATE_SYSTEM;
        

            case Freepool.FREEPOOL:
                freepool.receiveFreepool();
                return EXECUTE_TRANSITION;

            default:
                return DISCARD_EVENT;
        }
    }

    public static void main(String[] args) {
        new InputMachine();
    }
}