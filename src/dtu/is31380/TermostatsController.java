package dtu.is31380;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class TermostatsController extends AbstractHouseController {

  public TermostatsController() {
    super(1000); //set timestep to 1000ms
  }
 
  List<Termostat> termostats = new ArrayList<Termostat>();

  private Set<AbstractSpaceConfig> get_linked_rooms(RoomConfig room) {
	  Set<AbstractSpaceConfig> connected_rooms = new LinkedHashSet<AbstractSpaceConfig>();
	  for(DoorConfig door:room.getDoors()) {	//all doors in the room
			if (getInterface().getSensorValue(door.getSensors().get(0).name)!=0.0) {		//if doors are open
				ArrayList<AbstractSpaceConfig> linked_rooms = door.getLinkedSpaces();	//check between which rooms
				connected_rooms.add(linked_rooms.get(0));	//add all to common list as in this example rooms always combine same common space
				connected_rooms.add(linked_rooms.get(1));
			}
		}
	  return connected_rooms;
  }
  
  private Termostat find_termostat_in_room(String room_name) {
	  for(Termostat term:termostats) {
		  if (term.t_room_name.equals(room_name)) return term;
	  }
	  return termostats.get(0); // no error handing ;(
  }
  
  @Override
  protected void execute() {
    HouseControllerInterface intf=getInterface();	
    if (intf.getSimulationTime()>1) {
    	for (Termostat t:termostats) t.execute();
		ArrayList<RoomConfig> rooms=intf.getBuildingConfig().getRooms();
		Set<AbstractSpaceConfig> connected_rooms = new LinkedHashSet<AbstractSpaceConfig>();
		for(RoomConfig room:rooms) {
	    	connected_rooms.addAll(get_linked_rooms(room));	
		}
		double sum=0;
		for(AbstractSpaceConfig room:connected_rooms) {
			sum += find_termostat_in_room(room.name).goal_temp;	// sum up room "temperature goal"
		}																											 
		for(AbstractSpaceConfig room:connected_rooms) {
			find_termostat_in_room(room.name).goal_temp_door = sum/connected_rooms.size(); //assign average
		}
    }
  }

  private static final int[] ports = {8081,8082,8083,8084,8085,8086,8087,8088,8088};	// choose ports
  
  @Override
  protected void init() { 
    HouseControllerInterface bc=getInterface();
    ArrayList<RoomConfig> rooms=bc.getBuildingConfig().getRooms();
    int i = 0;
    for(RoomConfig room:rooms) {
    	Termostat room_termostat = new Termostat(room.name.toString(),ports[i]);
    	room_termostat.setInterface(bc); //also initialize 
    	termostats.add(room_termostat);
//    	System.out.println("Rooms: "+bc .toString());
    	i++;
    }
    System.out.println("Termostats inicjalization is done.");
  }
  
  

}
