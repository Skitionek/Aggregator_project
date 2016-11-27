package dtu.is31380.Communication.commexercise.pubsub.demo;

import java.text.SimpleDateFormat;
import java.util.Date;

import dtu.is31380.Communication.commexercise.pubsub.PubSubServer;

public class FlexibilityService implements Runnable {
    private PubSubServer broker;
    private Boolean running;
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private Thread thread;
	public static final String TOPIC = "Flexibility";

    public static final String FLEXIBILITY_ALL = "FlexAll";

    public FlexibilityService(PubSubServer server) {
        this.broker = server;
        this.thread = new Thread(this);
    }

    public void run() {
    	running = true;
        while(running) {
            broker.send(TOPIC, new String[]{FLEXIBILITY_ALL});
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { 
            	running = false;
            }
        }
    }

    public FlexibilityService start() {
        this.thread.start();
        return this;
    }
    
    public void stop() {
    	running = false;
        this.thread.interrupt();
    }
}
