package dtu.is31380;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import commexercise.rpc.CallListener;
	import commexercise.rpc.RpcServer;
	import commexercise.rpc.RpcServerImpl;

public class Termostat extends AbstractHouseController {

	public double goal_temp = 21;
	public double goal_temp_door = 21;
		  
	private RoomConfig t_room = null;
	public String t_room_name;
	private int port;
	private static final String SET_TEMP="set_t";
	
  public Termostat(String room_name,int portt) {
    super(1000); //set timestep to 1000ms
    t_room_name = room_name;
    port = portt;
    
    
    try {
    RpcServer server = new RpcServerImpl(port).start();
    
    System.out.println("Termostat in "+t_room_name+" was initiated on port: "+port);
    
    // add a call listener that will get called when a client does an RPC call to the server
    server.setCallListener(new CallListener() {
      @Override
      public String[] receivedSyncCall(String function, String[] fargs) throws Exception {
        if (function.equals(SET_TEMP)) {
          if ((fargs!=null) && (fargs.length>0)) {
            String name=fargs[0];
            if (name.equals(t_room_name)) {
	            goal_temp=Long.valueOf(fargs[1]).longValue();
	            goal_temp_door = goal_temp; //override door dependent temp in case master controller doesnt work 
	            System.out.println("Temperature in " + t_room_name + " set to "+goal_temp);
	            return new String[]{"Aknowlege> Temperature in " + t_room_name + " set to "+goal_temp};
            } else {
	            System.out.println("Name missmatch "+t_room_name+" != "+name.toString());
	            return new String[]{"Exception> Name missmatch "+t_room_name+" != "+name.toString()};
            }
          }
        }
        else {
          return new String[]{"Function '",function,"' does not exist."};
        }
        return null;
      }
      
      @Override
      public String[] receivedAsyncCall(String function, String[] args, long callID) throws Exception {
        return receivedSyncCall(function,args);
      }
    });
    } catch (Exception e) {
    	System.out.println("Termostat "+t_room_name+" was initiated on occupaid port! ("+port+")");
    }
  }

  private static final float histereza = 0; //acceptable error - to avoid frequent changes (set to zero as frequency limit is used) 
  
  private List<String> get_heaters_names() {
	List<String> names = new ArrayList<String>();
	for(ActuatorConfig actuator:t_room.getActuators()) {
		Matcher matcher = Pattern.compile("htr").matcher(actuator.name.toString()); // match heaters
		if (matcher.find()) {
			names.add(actuator.name.toString());				//collect names
		}
	}
	  return names;
  }  
  
  private String get_t_sens_name() { 
	for(SensorConfig sensor:t_room.getSensors()) {
		String name = sensor.name.toString();
		Matcher matcher = Pattern.compile("temp").matcher(name); // match temp
		if (matcher.find()) {
			return name;
		}
	}
	return "n/a";
  }
  
  @Override
  protected void execute() { //time start set in master
    HouseControllerInterface intf=getInterface();	
    if (t_room != null) {
		Boolean change = false;
		double state = 0.0,
			temp = intf.getSensorValue(get_t_sens_name()); //temperature in the room		
		if (temp<goal_temp_door-histereza) {		//compare with heater goal
			state = 1.0;	//switch heaters in room on
			change = true;
		} else if (goal_temp_door+histereza<temp) {
			state = 0.0;	//switch heaters in room off
			change = true;
		}	
		if (change) {
			List<String> heaters_names = get_heaters_names();
			for (String name:heaters_names) {
				intf.setActuator(name.toString(), state);  //turn off all heaters
			}	
		}
    }
  }

  @Override
  protected void init() { 
    ArrayList<RoomConfig> rooms=getInterface().getBuildingConfig().getRooms();
    for(RoomConfig room:rooms) {
    	if (room.name.equals(t_room_name)) t_room = room;
    }

	List<String> heaters_names = get_heaters_names();
	for (String name:heaters_names) {
		getInterface().setActuator(name.toString(), 0.0);  //turn off all heaters
	}	
	
//	System.out.println("Name: " + t_room.name + " Heaters: "+heaters_names.toString() + " T_sens: " + get_t_sens_name());
//	List<String> doors_names = new ArrayList<String>(); 
//	for(Config door:t_room.getDoors()) {
//		doors_names.add(door.name.toString());	//collect names for nice output
//		System.out.println("Have doors from "+door.getLinkedSpaces().get(0).name+" to "+door.getLinkedSpaces().get(1).name);
//	}
  }
  
  

}
