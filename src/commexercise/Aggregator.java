package commexercise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commexercise.pubsub.PubSubServer;
import commexercise.pubsub.PubSubServerImpl;
import commexercise.pubsub.PubSubSubscriberListener;
import commexercise.pubsub.demo.FlexibilityService;
import commexercise.rpc.CallListener;
import commexercise.rpc.RpcServer;
import commexercise.rpc.RpcServerImpl;

public class Aggregator {
	
    private long initTime = 0;

	private RpcServer grid_server = null;
	public static final int AGG_GRID_PORT = 8080;
	private static final String FUN_TIME_SYNC = Grid.FUN_TIME_SYNC; //keep it short
	private static final String FUN_REQUEST =  Grid.FUN_REQUEST; //keep it short
	private static final String FUN_ACTIVATION = Grid.FUN_ACTIVATION; //keep it short
	

	private RpcServer house_server = null;
	public static final int AGG_HOUSE_PORT = 8082;

    private PubSubServer pubSubServer;
    public static final int AGG_PORT_PUB = 9090;
	public static final String TOPIC = "Flexibility";
    public static final String FLEXIBILITY_ALL_AT_T0 = "FlexAll";

	private class Record {
    	public String homeName = null;
    	public double t0 = 0;
    	public double flexibility_time = 0;    
    	Record(String homeName,double t0,double flexibility_time) {
    		this.homeName = homeName;
    		this.t0 = t0;
    		this.flexibility_time = flexibility_time;
    	}
	}
    private class Records {
        public List<Record> list = null;
        public boolean valid = true;
    	Records() {
    		list = new ArrayList();
    	}
    }
    private Records records = null;
    
    
    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);

  private Aggregator() {   
	  	records = new Records();
	    // create an rpc server listening on port
	    try {
			grid_server = new RpcServerImpl(AGG_GRID_PORT).start();
			house_server = new RpcServerImpl(AGG_HOUSE_PORT).start();
		} catch (Exception e) {
			System.out.println("Server start exception!");			
			e.printStackTrace();
		} 

        // create a pubsub server listening on port 
        log.info("Starting PubSub Server");
        try {
			pubSubServer = new PubSubServerImpl(AGG_PORT_PUB).start();
		} catch (Exception e) {
			System.out.println("Publisher start exception");
			e.printStackTrace();
		}

        // add subscriber listener (gets called when a client subscribes or unsubscribes)
        pubSubServer.addSubscriberListener(new PubSubSubscriberListener() {
            public void subscriberJoined(String topic, String id) {
              System.out.println("Subscriber '"+id+"' joined for topic '"+topic);
            }

            public void subscriberLeft(String topic, String id) {
              System.out.println("Subscriber '"+id+"' left for topic '"+topic);
            }
        });

        // start the demo service that publishes current time to subscribers
        //ClockService clock = new ClockService(pubSubServer).start();
        new FlexibilityService(pubSubServer).start();
       
	    // add a call listener that will get called when a girp does an RPC call to the agg
        grid_server.setCallListener(new CallListener() {
  	      @Override
  	      public String[] receivedSyncCall(String function, String[] args) throws Exception {
  	        System.out.println("Received call for function '"+function+"' with arguments"+
  	                            Arrays.toString(args)+". Replying now.");
  	        if (function.equals(FUN_TIME_SYNC)) {
  	        	if(args.length==1) {
  	        		initTime = Long.valueOf(args[0]);
  	        		System.out.println("initTime set to "+initTime);
  	        		return new String[]{
  	    		        	"ACC"
  			        };
  	        	} else {
  	        		System.out.println("Wrong number of arguments!");
  	        		return new String[]{
  	        				"EXC"
  	        		};
  	        	}
  	        } else if (function.equals(FUN_REQUEST)) {
  	        	if(args.length==5) {
//  		        	startTime,endTime,maxDelay,minDuration,regType
  	        			
//  	        		Thinking if we can?
//  	        		whole logic goes here
  	        		pubSubServer.send(TOPIC, new String[]{FLEXIBILITY_ALL_AT_T0});
  	        		Thread.sleep(1000);
  	        		
  	        		//TODO !!!!!
  	        		
  	        		System.out.println("initTime set to "+initTime);
  	        		return new String[]{
  	        				String.valueOf(args[4]=="UP"?-100:100)
  			        };
  	        	} else {
  	        		System.out.println("Wrong number of arguments!");
  	        		return new String[]{
  	        				"EXC"
  	        		};
  	        	}
  	        } else if (function.equals(FUN_ACTIVATION)) {
  	        	
  	        	
  	        	
  	        	
  	        	return new String[]{
  	        		"You called:",function," with arguments ",Arrays.toString(args)
  	        	};
  	        } else {
  	        	return new String[]{
  	        		"Function '",function,"' does not exist."
  	        	};
  	        }
  	      }
  	      @Override
  	      public String[] receivedAsyncCall(String function, String[] args, long callID) throws Exception {
  	        return null; // not implemented for this test
  	      }
  	    });
        
        house_server.setCallListener(new CallListener() {
  	      @Override
  	      public String[] receivedSyncCall(String function, String[] args) throws Exception {
  	        System.out.println("Received call for function '"+function+"' with arguments"+
  	                            Arrays.toString(args)+". Replying now.");
  	        switch(function) {
  	        	case FUN_TIME_SYNC:
  	        	break;
  	        };
  	        if (records.valid) {
  	        	records.valid = false;
  	        	//Record current = new Record(args[0],Double.valueOf(args[1]),args[2]);
  	        	//records.list.add(current);
  	        }
  	        return new String[]{"ACC"};
  	      }
  	      @Override
  	      public String[] receivedAsyncCall(String function, String[] args, long callID) throws Exception {
  	        return null; // not implemented for this test
  	      }
  	    });
  }
  
  public static void main(String args[]) {
	  new Aggregator();
  }
  
}
