package Aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dtu.is31380.AbstractHouseController;
import dtu.is31380.ActuatorConfig;
import dtu.is31380.HouseControllerInterface;
import dtu.is31380.SensorConfig;



public class TestHouseController extends AbstractHouseController {

	private double T_set;
	private double deviation;
	private double T_bandmax;
	private double T_bandmin;
	private static double Tminreg;
	private static double Tmaxreg;
	private static boolean regulating;
    private static double average;
	private static String NAME = null;	
  	protected static final int TIME_STEP = 5000;

	
  public TestHouseController() {
    super(TIME_STEP); //set timestep to 5000ms
    T_set=21;
    deviation=0.25;
    T_bandmax=T_set+deviation;
    T_bandmin=T_set-deviation;
    Tminreg=19;
    Tmaxreg=24;
    regulating=false;
  }
  
  //ACTIVATION SIGNALS (TURN ON, OFF OF NORMAL)
	public enum State {
	    MAX, 
	    NORM,
	    MIN 
	  };
	  
private State state=State.NORM;

  @Override
  protected void execute() {
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
  }

 
  
  @Override
  protected void init() {
	  NAME="house"+Math.random();
	  System.out.println(NAME);
		
	HouseControllerInterface intf=getInterface();
    List<String> heaters=get_heaters();
    for (String heat:heaters){
    	intf.setActuator(heat.toString(), 0.0);
	}
  }

      private List<String> get_heaters(){
    		HouseControllerInterface intf=getInterface();
    		List<String> heaters=new ArrayList<String>();
    		for (ActuatorConfig actuator:intf.getBuildingConfig().getAllActuators()){
    			Matcher match=
    					Pattern.compile("htr").matcher(actuator.name.toString());
    			if (match.find()){
    				heaters.add(actuator.name.toString());
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
    					Pattern.compile("(tempr)|(tempm)").matcher(sensor.name.toString());
    			if (match.find()){
    				sensors.add(sensor.name.toString());
    			}
    		}
    		return sensors;
    	}

  	
  	protected static double get_time(String req){
  		
  		double T_out;
  		double Cdown;
  		double Cup;
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
  	
  	
  }



