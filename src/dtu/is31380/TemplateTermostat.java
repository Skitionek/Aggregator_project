package dtu.is31380;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import dtu.is31380.Communication.commexercise.rpc.CallListener;
import dtu.is31380.Communication.commexercise.rpc.RpcServer;
import dtu.is31380.Communication.commexercise.rpc.RpcServerImpl;

import java.util.regex.Matcher;

public class TemplateTermostat extends AbstractHouseController {

	public double goal_temp = 25;
	public double goal_temp_door = 25;
		  
	public RoomConfig t_room;
	private String t_room_name;
	private int port;
	private static final String SET_TEMP="set_t";
	
  public TemplateTermostat(String room_name,int portt) {
    super(1000); //set timestep to 1000ms
    t_room_name = room_name;
    port = portt;
    
    RpcServer server = null;
    try {
    	server = new RpcServerImpl(port).start();
    } catch (Exception e) {
    	System.out.println("Termostat "+t_room_name+" was initiated on occupaid port! ("+port+")");
    }
    // add a call listener that will get called when a client does an RPC call to the server
    server.setCallListener(new CallListener() {
      @Override
      public String[] receivedSyncCall(String function, String[] fargs) throws Exception {
        if (function.equals(SET_TEMP)) {
          if ((fargs!=null) && (fargs.length>0)) {
        	System.out.println(fargs.toString());
            String name=fargs[0];
            if (name.equals(t_room_name)) {
	            goal_temp=Long.valueOf(fargs[1]).longValue();
	            System.out.println("Temperature set to: "+goal_temp);
	            return new String[]{"Termostat "+name+" set to"+goal_temp};
            } else {
	            System.out.println("Name missmatch "+t_room_name+" != "+name.toString());
	            return new String[]{"Name missmatch, this termostat is called "+name,"Exception"};
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
  }

  private static final float histereza = 0; //acceptable error - to avoid frequent changes
  
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
  protected void execute() {
    HouseControllerInterface intf=getInterface();	
    if (intf.getSimulationTime()>1) {
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
	
	System.out.println("Name: " + t_room.name + " Heaters: "+heaters_names.toString() + " T_sens: " + get_t_sens_name());
	List<String> doors_names = new ArrayList<String>(); 
	for(DoorConfig door:t_room.getDoors()) {
		doors_names.add(door.name.toString());	//collect names for nice output
		System.out.println("Have doors which links "+door.getLinkedSpaces().get(0).name+" and "+door.getLinkedSpaces().get(1).name);
	}
  }
  
  

}
