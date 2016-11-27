package dtu.is31380.aggregator;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commexercise.pubsub.PubSubServer;
import commexercise.pubsub.PubSubServerImpl;
import commexercise.pubsub.PubSubSubscriberListener;
import commexercise.pubsub.demo.FlexibilityService;
import commexercise.rpc.CallListener;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;
import commexercise.rpc.RpcServer;
import commexercise.rpc.RpcServerImpl;

public class Aggregator {
	
    private long initTime = 0;

	private RpcClient grid_client = null;
	private static final String GRID_ADDRESS = "localhost";
	public static final int GRID_PORT = Grid.GRID_PORT;
    public static final String FLEXIBILITY_ALL = "FlexAll";
	
	private RpcServer grid_server = null;
	public static final int AGG_GRID_PORT = 8080;
	public static final String FUN_TIME_SYNC = Grid.FUN_TIME_SYNC; //keep it short
	private static final String FUN_REQUEST =  Grid.FUN_REQUEST; //keep it short
	private static final String FUN_ACTIVATION = Grid.FUN_ACTIVATION; //keep it short
	

	private RpcServer house_server = null;
	public static final int AGG_HOUSE_PORT = 8082;

    private PubSubServer pubSubServer;
    public static final int AGG_PORT_PUB = 9090;
	public static final String TOPIC = "Flexibility";
    public static final String FLEXIBILITY_ALL_AT_T0 = "FlexT0";
    public static final String ACTIVATE = "Activate";

	private class Record  implements Comparable<Record> {
    	public String homeName = null;
    	public double flexibility_time = 0; 
    	public double flexibility_power = 0;    
    	Record(String homeName,double flexibility_time, Double flexibility_power) {
    		this.homeName = homeName;
    		this.flexibility_time = flexibility_time;
    		this.flexibility_power = flexibility_power;
    	}
    	public String toString() {
    		return homeName+" "+flexibility_time+" "+flexibility_power;
    	}
    	public int compareTo(Record anotherInstance) {
            return (int) (this.flexibility_power - anotherInstance.flexibility_power);
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
			//e.printStackTrace();
		} 
		try {
			grid_client = new RpcClientImpl("http://"+GRID_ADDRESS+":"+GRID_PORT);
		} catch (MalformedURLException e) {
			System.out.println("Connection refused!");
			//e.printStackTrace();
		}

        // create a pubsub server listening on port 
        log.info("Starting PubSub Server");
        try {
			pubSubServer = new PubSubServerImpl(AGG_PORT_PUB).start();
		} catch (Exception e) {
			System.out.println("Publisher start exception");
			//e.printStackTrace();
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
        //new FlexibilityService(pubSubServer).start();
       
	    // add a call listener that will get called when a gird does an RPC call to the agg
        grid_server.setCallListener(new CallListener() {
  	      @Override
  	      public String[] receivedSyncCall(String function, String[] args) throws Exception {
  	        System.out.println("Received call for function '"+function+"' with arguments"+
  	                            Arrays.toString(args)+". Replying now.");
  	        if (function.equals(FUN_TIME_SYNC)) {
  	        	if(args.length==1) {
  	        		initTime = Long.valueOf(args[0]);
  	        		System.out.println("initTime set to "+initTime);
  	        		System.out.println("set initTime in house");
		  	      	String[] reply = null;
		  	  		// do synchronous call to agg TIME SYNCH
		  	  		pubSubServer.send(TOPIC, new String[]{FUN_TIME_SYNC, String.valueOf(initTime)});
		  	  		System.out.println("Success!");
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
                	String[] reply = null;
//  		        	startTime,endTime,maxDelay,minDuration,regType
  	        			
//  	        		Thinking if we can?
//  	        		whole logic goes here
  	        		pubSubServer.send(TOPIC, new String[]{FLEXIBILITY_ALL_AT_T0,args[4],args[0],args[1]});
  	        		
  	        		records.list.clear();
  	        		Thread.sleep(1000);
  	        		System.out.println("How u doinig?");
  	        		double delta_power = 0;
  	        		double bufor_t = 0;
  	        		double bufor_p = 10000000;
  	        		
  	        		Collections.sort(records.list);
  	        		for(Record record:records.list) {
  	        			if(record.flexibility_time>Double.valueOf(args[3])) delta_power+=record.flexibility_power;
  	        			else {
  	        				bufor_t+=record.flexibility_time;
  	        				bufor_p=Math.min(bufor_p,record.flexibility_power);
  	  	        			if(record.flexibility_time>Double.valueOf(args[3])) {
  	  	        				delta_power+=bufor_p;
  	  	        				bufor_t=0;
  	  	        				bufor_p=100000000;
  	  	        			}
  	        			}
  	        			System.out.println(record.toString());
  	        		}
  	        		
  	        		return new String[]{
  	        				String.valueOf(delta_power)
  			        };
  	        	} else {
  	        		System.out.println("Wrong number of arguments!");
  	        		return new String[]{
  	        				"EXC"
  	        		};
  	        	}
  	        } else if (function.equals(FUN_ACTIVATION)) {
	        	pubSubServer.send(TOPIC, new String[]{ACTIVATE,args[0]}); //to be change for sequenctional calls

  	        	
  	        	
  	        	
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
  	        System.out.println("Received call from house for function '"+function+"' with arguments"+
  	                            Arrays.toString(args)+". Replying now.");
  	        switch(function) {
	        	case FUN_TIME_SYNC:
	      	        return new String[]{"ACC"};
  	        	case FLEXIBILITY_ALL_AT_T0:
  	    	        if (args.length==3) {
  	    	        	//homeName,flexibility_time,flexibility_power
  	  	        	Record current = new Record(args[0],Double.valueOf(args[1]),Double.valueOf(args[2]));
  	  	        	records.list.add(current);
  	  	        		
  	  	  	        return new String[]{"ACC"};
  	    	        }
  	        	case ACTIVATE:
  	    	        return new String[]{"ACC"};
  	        };
  	        return new String[]{"EXC"};
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
