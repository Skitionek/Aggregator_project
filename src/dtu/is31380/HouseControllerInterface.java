package dtu.is31380;

public interface HouseControllerInterface {
  
  BuildingConfig getBuildingConfig();
  Sensor[] getSensors();
  Actuator[] getActuators();
  Sensor getSensorByName(String name);
  Actuator getActuatorByName(String name);
  
  Double getSimulationTime();
  
  Double getSensorValue(String name);
  Double getActuatorValue(String name);
  Double getActuatorSetpoint(String name);
  void setActuator(String name, Double value);
  
}
