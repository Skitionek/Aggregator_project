package dtu.is31380;

import java.util.ArrayList;

public abstract class AbstractSpaceConnectorConfig {

  protected String name;
  protected String connectedToName;
  protected double area;
  protected ArrayList<SensorConfig> sensors; //sensors associated with the connector
  protected ArrayList<ActuatorConfig> actuators; //actuators associated with the connector
  
  protected AbstractSpaceConnectorConfig() {
    sensors=new ArrayList<SensorConfig>();
    actuators=new ArrayList<ActuatorConfig>();
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
  
  public String getName() {
    return name;
  }

  public Double getArea() {
    return area;
  }
  
}
