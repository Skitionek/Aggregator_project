package dtu.is31380;

import java.util.ArrayList;

public class TestHouseController extends AbstractHouseController {

  public TestHouseController() {
    super(5000); //set timestep to 5000ms
  }
  
  @Override
  protected void execute() {
    HouseControllerInterface intf=getInterface();
    if (intf.getSimulationTime()>100) {
      if (intf.getActuatorSetpoint("a_htrr1_1")<0.5) {
        intf.setActuator("a_htrr1_1", 1.0); //switch heater in room 1 on
      }
    }
    System.out.println("T_room1="+intf.getSensorValue("s_tempr1"));
    
  }

  @Override
  protected void init() {
    BuildingConfig bc=getInterface().getBuildingConfig();
    ArrayList<RoomConfig> rooms=bc.getRooms();
    System.out.println("Rooms: "+rooms.toString());
    getInterface().setActuator("a_htrr1_1", 0.0);
  }
  
  

}
