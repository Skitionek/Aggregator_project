package dtu.is31380;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MyController extends AbstractHouseController {

  public MyController() {
    super(1000); //set timestep to 1000ms
  }

  private static final float histereza = 0; //acceptable error - to avoid frequent changes
  
  @Override
  protected void execute() {
    HouseControllerInterface intf=getInterface();	
    if (intf.getSimulationTime()>1) {
		ArrayList<RoomConfig> rooms=intf.getBuildingConfig().getRooms();
    	Set<AbstractSpaceConfig> connected_rooms = new LinkedHashSet<AbstractSpaceConfig>();	
		for(RoomConfig room:rooms) {
			double temp = intf.getSensorValue(room.getSensors().get(0).name.toString()); //temperature in the room
			double state = 0;			//heater on/off
			boolean change = false;		//heater state change
			for(ActuatorConfig actuator:room.getActuators()) { 
	    		Matcher matcher = Pattern.compile("htr").matcher(actuator.name.toString());
	    		if (matcher.find()) {
	    			double temp_goal = Double.parseDouble(actuator.getInterfaceAttributes().get("temp_goal")); 	//room temperature
	    			if (temp<temp_goal-histereza) {		//compare with heater goal
	    				state = 1.0;	//switch heaters in room on
	    				change = true;
	    			} else if (temp_goal+histereza<temp) {
	    				state = 0.0;	//switch heaters in room off
	    				change = true;
	    			}	
	    			if (change) {
	    				intf.setActuator(actuator.name.toString(),state);	//if needed change heater state
	    			}
	    			String tmp = actuator.getInterfaceAttributes().get("room_temp_goal");
	    			actuator.getInterfaceAttributes().replace("temp_goal", tmp);	//each heater got default goal
	    		}
	    	}
			for(DoorConfig door:room.getDoors()) {	//all doors in the room
				if (intf.getSensorValue(door.getSensors().get(0).name)!=0.0) {		//if doors are open
					ArrayList<AbstractSpaceConfig> linked_rooms = door.getLinkedSpaces();	//check between which rooms
					connected_rooms.add(linked_rooms.get(0));	//add all to common list as in this example rooms always combine same common space
					connected_rooms.add(linked_rooms.get(1));
				}
			}
		}
		double sum=0;
		int i = 0;
		for(AbstractSpaceConfig room:connected_rooms) {
			sum += Double.parseDouble(room.getActuators().get(0).getInterfaceAttributes().get("room_temp_goal"));	// sum up room "temperature goal" stored in heater attribute
			i++;
		}																											// its ugly work around as no attribute can be added to the room 
		for(AbstractSpaceConfig room:connected_rooms) {
			for(ActuatorConfig actuator:room.getActuators()) { 
		    		Matcher matcher = Pattern.compile("htr").matcher(actuator.name.toString()); 	//match only heaters
		    		if (matcher.find()) actuator.getInterfaceAttributes().replace("temp_goal", sum/i+"");	//each heater in set overwrite default goal by new goal - average temp
			}
		}
    }
  }

  private static final double[] temp_goals = {18.5,19,19.5,20,20.5,21,21.5,22};	//initial goals for rooms temperatures
  
  @Override
  protected void init() { 
    BuildingConfig bc=getInterface().getBuildingConfig();
    ArrayList<RoomConfig> rooms=bc.getRooms();
    //System.out.println("Rooms: "+bc .toString());
    int i = 0;
    for(RoomConfig room:rooms) {
    	List<String> actuator_names = new ArrayList<String>();
    	for(ActuatorConfig actuator:room.getActuators()) {
    		Matcher matcher = Pattern.compile("htr").matcher(actuator.name.toString());
    		if (matcher.find()) {
    			actuator_names.add(actuator.name.toString());	//collect names for nice output
    			actuator.getInterfaceAttributes().put("temp_goal", temp_goals[i]+"");		//global goal affected by the doors
    			actuator.getInterfaceAttributes().put("room_temp_goal", temp_goals[i]+"");	//goal independent to the doors state
    		}
    	}
    	System.out.println("Name: " + room.name + " Heaters: "+actuator_names.toString() + " T_sens: " + room.getSensors().get(0).name.toString());
    	List<String> doors_names = new ArrayList<String>(); 
    	for(DoorConfig door:room.getDoors()) {
    		doors_names.add(door.name.toString());	//collect names for nice output
    		System.out.println("Have doors which links "+door.getLinkedSpaces().get(0).name+" and "+door.getLinkedSpaces().get(1).name);
    	}
        getInterface().setActuator(room.getActuators().get(0).name.toString(), 0.0);	//turn off all heaters
        i++;
    }
  }
  
  

}
