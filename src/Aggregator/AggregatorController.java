package Aggregator;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.EmptyStackException;

import dtu.is31380.Communication.commexercise.pubsub.*;
import dtu.is31380.Communication.commexercise.rpc.RpcClient;
import dtu.is31380.Communication.commexercise.rpc.RpcClientImpl;

public class AggregatorController extends HouseController {
	
	private static final String AGG_ADDRESS = "localhost";
	private static final int AGG_PORT = Aggregator.AGG_HOUSE_PORT;
	private static final int AGG_PORT_PUB = Aggregator.AGG_PORT_PUB;
	private static RpcClient client = null;
	private static double flexibility_time = 1;
	private static final double flexibility_power = 1; //kW
	
	public static final String FUN_TIME_SYNC = Aggregator.FUN_TIME_SYNC;
    public static final String FLEXIBILITY_ALL_AT_T0 = Aggregator.FLEXIBILITY_ALL_AT_T0;
    public static final String ACTIVATE = Aggregator.ACTIVATE;
    
	private static void wait_random(long ms) {
		try {Thread.sleep((long) (ms+ms*Math.random()));} catch (InterruptedException e1) {};
	}
	private static void exc(String s,long ms) {
		System.out.println(s);
		wait_random(ms);
	}
	private static void exc(String s) {
		exc(s+" Trying again in random time of 1-2s...",1000);
	}
	
	public AggregatorController() {
	  	for(int i=1;i<=10;i++) try {
			client = new RpcClientImpl("http://"+AGG_ADDRESS+":"+AGG_PORT);
			i=10;
		} catch (MalformedURLException e) {
			exc(i+". Connection to aggregator refused! Check address and port.");
		}
	  	for(int i=1;i<=10;i++) try {
			suscribeToAggregator();
			i=10;
		} catch (Exception e) {
			exc(i+". Subscription fail!");
		}
    };		
    
	public static void suscribeToAggregator() throws Exception {
		// create a pubsub client
        PubSubClient pubSubClient = new PubSubClientImpl(AGG_ADDRESS,AGG_PORT_PUB,NAME);

        // subscribe to flexibility topic
        PubSubCallbackListener flexibilityListener = new PubSubCallbackListener() {
            public void messageReceived(String[] msg) {
                System.out.println("aggregator>>" + Arrays.toString(msg));
            	String[] reply = null;
                switch(msg[0]) {
                case FUN_TIME_SYNC:
      	        	if(msg.length==2) {
      	        		initTime = Long.valueOf(msg[1]);
      	        		System.out.println("InitTime set to "+initTime+".");
      	        		try {
							reply = client.callSync(FUN_TIME_SYNC, new String[]{"ACC"});
						} catch (Exception e) {}
      	        	} else {
      	        		System.out.println("Wrong number of arguments in function '"+msg[0]+"'!");
      	        		try {
							reply = client.callSync(FUN_TIME_SYNC, new String[]{"EXC"});
						} catch (Exception e) {}
      	        	}
                break;                
                case FLEXIBILITY_ALL_AT_T0:
                	RegulationType type = msg[1]=="UP"?RegulationType.UP:RegulationType.DOWN;
                	double t_start = Double.valueOf(msg[2]);
                	double t_end = Double.valueOf(msg[3]);
            		// do synchronous call to agg
        			for(int i=1;i<=10;i++) try {
	          	        	//homeName,flexibility_time
	        				try {
	            				flexibility_time = flexibility_at_t0(type,t_start,t_end);
	        					if (flexibility_time>=0) System.out.println("House flexibility: "+flexibility_time);
	        				} catch (Exception e) {
	        					exc("Flexibility equals null. Probably simulator is down.");
	        					flexibility_time = 0;
	        				}
	        				reply = client.callSync(FLEXIBILITY_ALL_AT_T0, new String[]{
	        						NAME,
	        						String.valueOf(flexibility_time),
	        						String.valueOf(flexibility_power)
	        				});
	        				if (!reply[0].equals("ACC")) throw new EmptyStackException();            					
	        				i=10;
	        			} catch (Exception e) {
	        				exc("Flexibility call exception! Possibly port is used or accknowledge message dropped!");
        			}
                	break;
                case ACTIVATE:
                	RegulationType act_type = msg[1]=="UP"?RegulationType.UP:RegulationType.DOWN;
            		// do synchronous call to agg
        			try {
	          	        	//homeName,flexibility_time
	        				if (act_type==RegulationType.UP) state=State.MAX;
	        				if (act_type==RegulationType.DOWN) state=State.MIN;
	        				reply = client.callSync(ACTIVATE, new String[]{"ACC"});
	        			} catch (Exception e) {
	        				exc("Activation call exception! Possibly port is used!");
        			}
        			System.out.println("Home state "+state);	
                	break;
                }             
            }
        };
        System.out.println("Subscribing to aggregator service.");
        pubSubClient.subscribe("Flexibility", flexibilityListener);


//	        System.out.println("Unsubscribing from aggregator service.");
//	        pubSubClient.unsubscribe(clockListener);
//	        pubSubClient.stop();	      
  }

    public static void main(String args[]) throws Exception {  // to run it from commandline
  	  AggregatorController house = new AggregatorController();
  	  house.init();
  	  while(true) {
  		  house.execute();
  		  Thread.sleep(TIME_STEP);
  	  }
    }
}
