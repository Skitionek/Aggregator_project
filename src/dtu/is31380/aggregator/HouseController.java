package dtu.is31380.aggregator;

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
import dtu.is31380.BuildingConfig;
import dtu.is31380.HouseControllerInterface;
import dtu.is31380.RoomConfig;
import dtu.is31380.SensorConfig;

public class HouseController extends AbstractHouseController {

//	##################YOUR CODE GOES HERE##################################
	private double T_set;
	private double deviation;
	private double T_bandmax;
	private double T_bandmin;
	private double Tminreg;
	private double Tmaxreg;
	private boolean regulating;
    double average;
  	private static final float flexibility = 1;
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
	public enum RegulationType {
	    MAX, 
	    NORM,
	    MIN 
	  };
	private RegulationType regulation=RegulationType.NORM;
	  
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
    	        regulation=RegulationType.NORM;
    	        }
    	    
    	    if (regulation==RegulationType.NORM){
    	    	
    	    
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
    	    
    	    else if (regulation==RegulationType.MAX){
    	    	regulating=true;
    	    	for (String heat:heaters){
    	    	intf.setActuator(heat.toString(), 1.0);
    	    	}
    	    	
    		    	if (average>T_bandmax) {
    		    		for (String heat:heaters){
    		    			intf.setActuator(heat.toString(), 0.0);
    		    			regulation=RegulationType.NORM;//CHANGE TO NORM
    		    		}
    		    	}
    		    }
    	    else if (regulation==RegulationType.MIN){
    	    	regulating=true;
    	    	for (String heat:heaters){
    	    	intf.setActuator(heat.toString(), 0.0);
    	    	}
    		    	if (average<T_bandmin) {
    		    		for (String heat:heaters){
    		    			intf.setActuator(heat.toString(), 1.0);
    		    			regulation=RegulationType.NORM;//CHANGE TO NORM
    		    			
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

	private double get_time(String req){
		
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
