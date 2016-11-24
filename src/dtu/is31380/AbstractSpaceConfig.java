package dtu.is31380;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class AbstractSpaceConfig {

  protected String name;
  protected ArrayList<SensorConfig> sensors; //sensors associated with the environment
  protected ArrayList<ActuatorConfig> actuators; //actuators associated with the environment
  protected HashMap<AbstractSpaceConfig,ArrayList<AbstractSpaceConnectorConfig>> adjacentSpaces;
            //table of which other spaces are adjacent through which doors/windows
 
  protected AbstractSpaceConfig() {
    adjacentSpaces=new HashMap<AbstractSpaceConfig,ArrayList<AbstractSpaceConnectorConfig>>();
  }
  
  protected void addSensorConfig(SensorConfig sc) {
    sensors.add(sc);
  }
  
  protected void addActuatorConfig(ActuatorConfig ac) {
    actuators.add(ac);
  }
  
  public ArrayList<SensorConfig> getSensors() {
    return sensors;
  }
  
  public ArrayList<ActuatorConfig> getActuators() {
    return actuators;
  }

  protected void addAdjacentSpace(AbstractSpaceConfig asc, AbstractSpaceConnectorConfig conn) {
    if (!adjacentSpaces.containsKey(asc)) {
      adjacentSpaces.put(asc, new ArrayList<AbstractSpaceConnectorConfig>());
    }
    ArrayList<AbstractSpaceConnectorConfig> connectorList=adjacentSpaces.get(asc);
    if (connectorList.contains(conn)) {
      System.out.println("Space '"+name+"' is already connected to space '"+asc.name+"' through connector '"
      +conn.name+"'. This should never happen.");
      return;
    }
    else {
      connectorList.add(conn);
    }
  }
  
  public HashMap<AbstractSpaceConfig,ArrayList<AbstractSpaceConnectorConfig>> getAdjacentSpaces() {
    return adjacentSpaces;
  }
  
  public String getName() {
    return name;
  }
  
}
