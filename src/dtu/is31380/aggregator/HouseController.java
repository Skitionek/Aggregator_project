package dtu.is31380.aggregator;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import commexercise.pubsub.*;
import commexercise.pubsub.demo.FlexibilityService;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;
import dtu.is31380.AbstractHouseController;
import dtu.is31380.BuildingConfig;
import dtu.is31380.HouseControllerInterface;
import dtu.is31380.RoomConfig;

public class HouseController extends AbstractHouseController {

	private static final int TIME_STEP = 5000;
	  public HouseController() {
	    super(TIME_STEP); //set timestep to 5000ms
	  }
	private static boolean dryRun = false;
	  
	public enum RegulationType {
	    MAX, 
	    NORM,
	    MIN 
	  };
	  
	private static String NAME = null;	
	private static final String TOPIC = FlexibilityService.TOPIC; 
	private static final String AGG_ADDRESS = "localhost";
	private static final int AGG_PORT = Aggregator.AGG_HOUSE_PORT;
	private static final int AGG_PORT_PUB = Aggregator.AGG_PORT_PUB;
	private static RpcClient client = null;
	private static final float flexibility_time = 1;

    public static final String FLEXIBILITY_ALL_AT_T0 = Aggregator.FLEXIBILITY_ALL_AT_T0;
    
    @Override
    protected void execute() {
      if(!dryRun) {
	      HouseControllerInterface intf=getInterface();
	      if (intf.getSimulationTime()>100) {
	        if (intf.getActuatorSetpoint("a_htrr1_1")<0.5) {
	          intf.setActuator("a_htrr1_1", 1.0); //switch heater in room 1 on
	        }
	      }
	      System.out.println("T_room1="+intf.getSensorValue("s_tempr1"));
      }
    }
    
    @Override
	protected void init() {
        if(!dryRun) {
        	BuildingConfig bc=getInterface().getBuildingConfig();
            ArrayList<RoomConfig> rooms=bc.getRooms();
            System.out.println("Rooms: "+rooms.toString());
            getInterface().setActuator("a_htrr1_1", 0.0);
        }
        
		NAME="house"+Math.random();
		try {
			client = new RpcClientImpl("http://"+AGG_ADDRESS+":"+AGG_PORT);
		} catch (MalformedURLException e) {
			System.out.println("Connection refused!");
			//e.printStackTrace();
		}
		try {
			suscribeToAggregator();
		} catch (Exception e) {
			System.out.println("Subscription fail!");
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
    };
	
	public static void suscribeToAggregator() throws Exception {
		// create a pubsub client
        PubSubClient pubSubClient = new PubSubClientImpl(AGG_ADDRESS,AGG_PORT_PUB,NAME);

        // subscribe to clock topic
        PubSubCallbackListener flexibilityListener = new PubSubCallbackListener() {
            public void messageReceived(String[] msg) {
                System.out.println("Received: " + msg[0]);
                switch(msg[0]) {
                case FLEXIBILITY_ALL_AT_T0:
                	double t0 = Double.valueOf(msg[1]);
                	String[] reply = null;
            		// do synchronous call to agg
            		while(reply==null) {
            			try {
              	        	//homeName,flexibility_time
            				reply = client.callSync(FLEXIBILITY_ALL_AT_T0, new String[]{NAME,String.valueOf(flexibility_time)});
            			} catch (Exception e) {
            				System.out.println("flexibility call exception! Possibly port is used!");
            				System.out.println("Retrying again in in random time (0-1s)...");
            				try {
            					Thread.sleep((long) (Math.random()));
            				} catch (InterruptedException e1) {
            					// TODO Auto-generated catch block
            					//e1.printStackTrace();
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
  	  HouseController house = new HouseController();
  	  house.dryRun=true;
  	  house.init();
  	  while(true) {
  		  house.execute();
  		  Thread.sleep(TIME_STEP);
  	  }
    }
}
