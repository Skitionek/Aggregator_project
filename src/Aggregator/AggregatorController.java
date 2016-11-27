package Aggregator;

import java.net.MalformedURLException;
import java.util.Arrays;

import dtu.is31380.Communication.commexercise.pubsub.*;
import dtu.is31380.Communication.commexercise.rpc.RpcClient;
import dtu.is31380.Communication.commexercise.rpc.RpcClientImpl;

public class AggregatorController extends TestHouseController {

    private static long initTime = 0;

	//ACTIVATION SIGNALS (TURN ON, OFF OF NORMAL)
	public enum State {
	    MAX, 
	    NORM,
	    MIN 
	  };
	private static State state=State.NORM;
	
	//ACTIVATION SIGNALS (TURN ON, OFF OF NORMAL)
	public enum RegulationType {
	    UP,  
	    DOWN
	  };
	private static String NAME = null;	
	private static final String AGG_ADDRESS = "localhost";
	private static final int AGG_PORT = Aggregator.AGG_HOUSE_PORT;
	private static final int AGG_PORT_PUB = Aggregator.AGG_PORT_PUB;
	private static RpcClient client = null;
	private static double flexibility_time = 1;
	private static final double flexibility_power = 1; //kW
	
	public static final String FUN_TIME_SYNC = Aggregator.FUN_TIME_SYNC;
    public static final String FLEXIBILITY_ALL_AT_T0 = Aggregator.FLEXIBILITY_ALL_AT_T0;
    public static final String ACTIVATE = Aggregator.ACTIVATE;
    
	  public AggregatorController() {
		  NAME="house"+Math.random();
			try {
				client = new RpcClientImpl("http://"+AGG_ADDRESS+":"+AGG_PORT);
			} catch (MalformedURLException e) {
				System.out.println("Connection refused!");
			}
			try {
				suscribeToAggregator();
			} catch (Exception e) {
				System.out.println("Subscription fail!");
			}
	    };

		private static double flexibility_at_t0(RegulationType type,double t_start,double t_end) {
			if(type==RegulationType.UP)
				return get_time("up");
			else 
				return get_time("down");
		}
		
		public static void suscribeToAggregator() throws Exception {
			// create a pubsub client
	        PubSubClient pubSubClient = new PubSubClientImpl(AGG_ADDRESS,AGG_PORT_PUB,NAME);

	        // subscribe to clock topic
	        PubSubCallbackListener flexibilityListener = new PubSubCallbackListener() {
	            public void messageReceived(String[] msg) {
	                System.out.println("Received: " + msg[0]);
	            	String[] reply = null;
	                switch(msg[0]) {
	                case FUN_TIME_SYNC:
	      	        	if(msg.length==2) {
	      	        		initTime = Long.valueOf(msg[1]);
	      	        		System.out.println("initTime set to "+initTime);
	      	        		try {
								reply = client.callSync(FUN_TIME_SYNC, new String[]{"ACC"});
							} catch (Exception e) {
								// TODO 
							}
	      	        	} else {
	      	        		System.out.println("Wrong number of arguments!");
	      	        		try {
								reply = client.callSync(FUN_TIME_SYNC, new String[]{"ACC"});
							} catch (Exception e) {
								// TODO 
							}
	      	        	}
	                break;                
	                case FLEXIBILITY_ALL_AT_T0:
	                	RegulationType type = msg[1]=="UP"?RegulationType.UP:RegulationType.DOWN;
	                	double t_start = Double.valueOf(msg[2]);
	                	double t_end = Double.valueOf(msg[3]);
	            		// do synchronous call to agg
	            		while(reply==null) {
	            			try {
	              	        	//homeName,flexibility_time
	            				try {
	                				flexibility_time = flexibility_at_t0(type,t_start,t_end);
	            					if (flexibility_time>=0) System.out.println("House flexibility: "+flexibility_time);
	            				} catch (Exception e) {
	            					System.out.println("Flexibility equals null. Probably simulator is down.");
	            					flexibility_time = 0;
	            				}
	            				reply = client.callSync(FLEXIBILITY_ALL_AT_T0, new String[]{
	            						NAME,
	            						String.valueOf(flexibility_time),
	            						String.valueOf(flexibility_power)
	            				});
	            			} catch (Exception e) {
	            				System.out.println("Flexibility call exception! Possibly port is used!");
	            				System.out.println("Retrying again in in random time (0-1s)...");
	            				try {
	            					Thread.sleep((long) (1000*Math.random()));
	            				} catch (InterruptedException e1) {
	            					// TODO 
	            				}
	            				//e.printStackTrace();
	            			}
	            			System.out.println(Arrays.toString(reply));	
	            		}
	                	break;
	                case ACTIVATE:
	                	RegulationType act_type = msg[1]=="UP"?RegulationType.UP:RegulationType.DOWN;
	            		// do synchronous call to agg
	            		while(reply==null) {
	            			try {
	              	        	//homeName,flexibility_time
	            				if (act_type==RegulationType.UP) state=State.MAX;
	            				if (act_type==RegulationType.DOWN) state=State.MIN;
	            				reply = client.callSync(ACTIVATE, new String[]{"ACC"});
	            			} catch (Exception e) {
	            				System.out.println("Activation call exception! Possibly port is used!");
	            				System.out.println("Retrying again in in random time (0-1s)...");
	            				try {
	            					Thread.sleep((long) (1000*Math.random()));
	            				} catch (InterruptedException e1) {
	            					// TODO 
	            				}
	            				//TODO
	            			}
	            			System.out.println(Arrays.toString(reply));	
	            			System.out.println("Home state "+state);	
	            		}
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
