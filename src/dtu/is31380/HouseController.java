package commexercise;

import java.net.MalformedURLException;
import java.util.Arrays;

import commexercise.pubsub.*;
import commexercise.pubsub.demo.FlexibilityService;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;

public class HouseController {
	
	public enum RegulationType {
	    MAX, 
	    NORM,
	    MIN 
	  };
	  
	private static String NAME = null;	
	private static final String TOPIC = FlexibilityService.TOPIC; 
	private static final int AGG_PORT = Aggregator.AGG_HOUSE_PORT;
	private static final int AGG_PORT_PUB = Aggregator.AGG_PORT_PUB;
	private static String AGG_ADDRESS = "localhost";
	private static RpcClient client = null;
	private static final float flexibility = 1;

    public static final String FLEXIBILITY_ALL = FlexibilityService.FLEXIBILITY_ALL;

	public HouseController() throws Exception { //TODO to be change to init()
		NAME="house1";
		try {
			client = new RpcClientImpl("http://"+AGG_ADDRESS+":"+AGG_PORT);
		} catch (MalformedURLException e) {
			System.out.println("Connection refused!");
			//e.printStackTrace();
		}
		suscribeToAggregator();
    };
	
	public static void suscribeToAggregator() throws Exception {
		// create a pubsub client
        PubSubClient pubSubClient = new PubSubClientImpl(AGG_ADDRESS,AGG_PORT_PUB,NAME);

        // subscribe to clock topic
        PubSubCallbackListener flexibilityListener = new PubSubCallbackListener() {
            public void messageReceived(String[] msg) {
                System.out.println("Received: " + msg[0]);
                switch(msg[0]) {
                case FLEXIBILITY_ALL:
                	String[] reply = null;
            		// do synchronous call to agg
            		while(reply==null) {
            			try {
            				reply = client.callSync(FLEXIBILITY_ALL, new String[]{String.valueOf(flexibility)});
            			} catch (Exception e) {
            				System.out.println("flexibility call exception! Possibly port is used!");
            				System.out.println("Retrying again in in random time (0-1s)...");
            				try {
            					Thread.sleep((long) (Math.random()));
            				} catch (InterruptedException e1) {
            					// TODO Auto-generated catch block
            					e1.printStackTrace();
            				}
            				//e.printStackTrace();
            			}
            			System.out.println(Arrays.toString(reply));	
            		}
                	break;
                }
                
                
                
                
                
                
                
            }
        };
        System.out.println("Subscribing to aggregator service.");
        pubSubClient.subscribe("Flexibility", flexibilityListener);


//        System.out.println("Unsubscribing from aggregator service.");
//        pubSubClient.unsubscribe(clockListener);
//        pubSubClient.stop();	      
	}
	
    public static void main(String args[]) throws Exception {  // to run it from commandline
  	  new HouseController();
    }
}
