
import mqtt.MQTTclient;

import runtime.IStateMachine;
import runtime.Scheduler;

public class InputMachine implements IStateMachine {
    public static final String TOPIC = "ttm4160-team1";

    /**
     * State: Idle - setting up the MQTTClient
     * State: Active - MQTTClient ready to receive messages
     */
    private static enum State {
        Idle, Active
    };

    private State state = State.Idle;

    private Scheduler scheduler;
    private MQTTclient client;

    InputMachine() {
        scheduler = new Scheduler(this);
        client = new MQTTclient("tcp://broker.hivemq.com:1883", "ttm4160-team1-inputmachine", true, scheduler);
        new Input(scheduler);
        scheduler.start();
    }

    @Override
    public int fire(String event, Scheduler scheduler) {
        if (event.startsWith(Input.INPUT)) {
            switch (state) {
                case Active:
                    String message = event.substring(Input.INPUT.length());
                    client.sendMessage(TOPIC, message);
                    this.state = State.Active;
                    return EXECUTE_TRANSITION;
                default:
                    return DISCARD_EVENT;
            }
        }

        switch (event) {
            case MQTTclient.READY:
                switch (state) {
                    case Idle:
                        state = State.Active;
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
        new InputMachine();
    }
}