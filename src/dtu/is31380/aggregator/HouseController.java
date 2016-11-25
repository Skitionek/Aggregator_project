package dtu.is31380.aggregator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import commexercise.pubsub.*;
import commexercise.pubsub.demo.FlexibilityService;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;
import dtu.is31380.AbstractHouseController;
import dtu.is31380.ActuatorConfig;
import dtu.is31380.HouseControllerInterface;
import dtu.is31380.SensorConfig;

public class HouseController extends AbstractHouseController {

    private static long initTime = 0;
    
//	##################YOUR CODE GOES HERE##################################
	private double T_set;
	private double deviation;
	private double T_bandmax;
	private double T_bandmin;
	private static double Tminreg;
	private static double Tmaxreg;
	private static boolean regulating;
    private static double average;
  	private static double flexibility = 1;
//  #######################################################################
    
	private static final int TIME_STEP = 5000;
	  public HouseController() {
		    super(TIME_STEP); //set timestep to 5000ms
  	
	    T_set=21;
	    deviation=0.25;
	    T_bandmax=T_set+deviation;
	    T_bandmin=T_set-deviation;
	    Tminreg=19;
	    Tmaxreg=24;
	    regulating=false;
	  }
	private static boolean dryRun = false;

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
	private RegulationType regulation=RegulationType.UP;
	  
	private static String NAME = null;	
	private static final String TOPIC = FlexibilityService.TOPIC; 
	private static final String AGG_ADDRESS = "localhost";
	private static final int AGG_PORT = Aggregator.AGG_HOUSE_PORT;
	private static final int AGG_PORT_PUB = Aggregator.AGG_PORT_PUB;
	private static RpcClient client = null;
	private static final float flexibility_time = 1;
	
	public static final String FUN_TIME_SYNC = Aggregator.FUN_TIME_SYNC;
    public static final String FLEXIBILITY_ALL_AT_T0 = Aggregator.FLEXIBILITY_ALL_AT_T0;
    public static final String ACTIVATE = Aggregator.ACTIVATE;

    
    @Override
    protected void execute() {
      if(!dryRun) {
//        ##################YOUR CODE GOES HERE##################################
    	  HouseControllerInterface intf=getInterface();
    	    List<String> heaters=get_heaters();
    	    List<String> sensors=get_sensors();
    	    double sum = 0;
    	    average=0;
    	    
    	    for (String sens:sensors){
    	    	sum += intf.getSensorValue(sens);
    	    	average = sum/sens.length();
    	    }
    	    System.out.println("Temp "+average);
    	    if (regulating && average>T_bandmin && average<T_bandmax){
    	        regulating=false;	
    	        state=State.NORM;
    	        }
    	    
    	    if (state==State.NORM){
    	    	
    	    
    	    	if (average<T_bandmin) {
    	    	
    	    		for (String heat:heaters){
    	    			intf.setActuator(heat.toString(), 1.0);
    	    		}
    	    	} else if (average>T_bandmax) {
    	    		for (String heat:heaters){
    	    			intf.setActuator(heat.toString(), 0.0);
    	    		}
    	    	}
    	    }
    	    
    	    else if (state==State.MAX){
    	    	regulating=true;
    	    	for (String heat:heaters){
    	    	intf.setActuator(heat.toString(), 1.0);
    	    	}
    	    	
    		    	if (average>T_bandmax) {
    		    		for (String heat:heaters){
    		    			intf.setActuator(heat.toString(), 0.0);
    		    			state=State.NORM;//CHANGE TO NORM
    		    		}
    		    	}
    		    }
    	    else if (state==State.MIN){
    	    	regulating=true;
    	    	for (String heat:heaters){
    	    	intf.setActuator(heat.toString(), 0.0);
    	    	}
    		    	if (average<T_bandmin) {
    		    		for (String heat:heaters){
    		    			intf.setActuator(heat.toString(), 1.0);
    		    			state=State.NORM;//CHANGE TO NORM
    		    			
    		    		}
    		    	}
    		    }
    	    System.out.println(average);
//        #######################################################################
      }
    }
//	##################YOUR CODE GOES HERE##################################
    private List<String> get_heaters(){
		HouseControllerInterface intf=getInterface();
		List<String> heaters=new ArrayList<String>();
		for (ActuatorConfig actuator:intf.getBuildingConfig().getAllActuators()){
			Matcher match=
					Pattern.compile("htr").matcher(actuator.getName().toString());
			if (match.find()){
				heaters.add(actuator.getName().toString());
			}
		}
		return heaters;
	}

	private List<String> get_sensors(){
		HouseControllerInterface intf=getInterface();
		List<String> sensors=new ArrayList<String>();
		for (SensorConfig sensor:intf.getBuildingConfig().getAllSensors()){
			Matcher match=
					//Pattern.compile("temp").matcher(sensor.name.toString());
					Pattern.compile("(tempr)|(tempm)").matcher(sensor.getName().toString());
			if (match.find()){
				sensors.add(sensor.getName().toString());
			}
		}
		return sensors;
	}
	private static double flexibility_at_t0(RegulationType type,double t_start,double t_end) {
		if(type==RegulationType.UP)
			return get_time("up");
		else 
			return get_time("down");
	}
	private static double get_time(String req){
		
		double T_out;
		double Cdown;
		double Cup;
		double c;
		double time=0;
		double timeup=0;
		double timedown=0;
		
		HouseControllerInterface intf=getInterface();
		T_out=intf.getSensorValue("s_tempout");
		
		if (!regulating){
		
		Cdown=0.0028*T_out;
		Cup=0.000382*T_out;
		
		timeup=(average-Tminreg)/Cup;
		timedown=(Tmaxreg-average)/Cdown;
		
		if (req=="up"){
				return timeup;
		}
		
		else if (req=="down"){
			return timedown;
	}
		
		else {
			return 0;
		}
		}
		else {
			return 0;
		}
	}
//  #######################################################################
    
    @Override
	protected void init() {
        if(!dryRun) {
//        	##################YOUR CODE GOES HERE##################################
      	  NAME="house"+Math.random();
    	  System.out.println(NAME);
    		
    	HouseControllerInterface intf=getInterface();
        List<String> heaters=get_heaters();
        for (String heat:heaters){
        	intf.setActuator(heat.toString(), 0.0);
    	}
//          #######################################################################
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
            	String[] reply = null;
                switch(msg[0]) {
                case FUN_TIME_SYNC:
      	        	if(msg.length==2) {
      	        		initTime = Long.valueOf(msg[1]);
      	        		System.out.println("initTime set to "+initTime);
      	        		try {
							reply = client.callSync(FUN_TIME_SYNC, new String[]{"ACC"});
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
      	        	} else {
      	        		System.out.println("Wrong number of arguments!");
      	        		try {
							reply = client.callSync(FUN_TIME_SYNC, new String[]{"ACC"});
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
            				flexibility = flexibility_at_t0(type,t_start,t_end);
            				System.out.println(flexibility);
            				reply = client.callSync(FLEXIBILITY_ALL_AT_T0, new String[]{NAME,String.valueOf(flexibility)});
            			} catch (Exception e) {
            				System.out.println("Flexibility call exception! Possibly port is used!");
            				System.out.println("Retrying again in in random time (0-1s)...");
            				try {
            					Thread.sleep((long) (1000*Math.random()));
            				} catch (InterruptedException e1) {
            					// TODO Auto-generated catch block
            					//e1.printStackTrace();
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
