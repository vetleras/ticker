package mqtt;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import runtime.Scheduler;

public class MQTTclient implements MqttCallback {

	private Scheduler scheduler;
	private MqttClient client;

	public static final String READY = "MQTTReady", ERROR = "MQTTError",
			MESSAGE = "MESSAGE";

	public MQTTclient(String broker, String myAddress, boolean conf, Scheduler s) {
		scheduler = s;
		MemoryPersistence pers = new MemoryPersistence();
		try {
			client = new MqttClient(broker, myAddress, pers);
			MqttConnectOptions opts = new MqttConnectOptions();
			opts.setCleanSession(true);
			client.connect(opts);
			client.setCallback(this);
			scheduler.addToQueueLast(READY);
		} catch (MqttException e) {
			System.err.println("MQTT Exception: " + e);
			scheduler.addToQueueLast(ERROR);
		}
	}

	public void connectionLost(Throwable e) {
		System.err.println("MQTT Connection lost: " + e);
		scheduler.addToQueueLast(ERROR);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		// System.out.println("Delivery completed")
	}

	public void messageArrived(String topic, MqttMessage mess) {
		scheduler.addToQueueLast(MESSAGE + new String(mess.getPayload(), StandardCharsets.UTF_8));
	}

	public void sendMessage(String topic, String content) {
		if (!this.client.isConnected()) {
			System.out.println("Client not connected");
			return;
		}
		MqttMessage message = new MqttMessage(content.getBytes());
		message.setQos(2);
		try {
			this.client.publish(topic, message);
			System.out.println("Message published");
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();
		}
	}

	public void subscribeToTopic(String topic) {
		try {
			this.client.subscribe(topic);
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();
		}
	}
}
