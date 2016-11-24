package dtu.is31380;

import java.util.ArrayList;
import java.util.HashMap;

public class HWModule {
  private BuildingConfig building;
  private ProcessImage procimg;
  private LoggerManager loggerManager;
  private HashMap<String,HardwareInterface> interfaces;
  private HashMap<String, HardwareInterface> deviceInterfaceMap;

  /**
   * Starts up a new HWModule using the configuration supplied in a XML file and a reference to a dormant
   * LoggerManager
   * @param cfgfile the path to an XML file describing the building
   * @param loggerMan a dormant(not yet started) LoggerManager
   */
  public HWModule(String cfgfile, LoggerManager loggerMan, long simtimeref) {
    interfaces = new HashMap<String,HardwareInterface>();
    building = new BuildingConfig();
    building.parseConfig(cfgfile);
    deviceInterfaceMap = new HashMap<String, HardwareInterface>();
    procimg = new ProcessImage(this, simtimeref);
    fillProcessImage();

    // Ready LoggerManager and start it
    loggerManager = loggerMan;
    loggerManager.startLoggers(procimg, building);

    //loggerManager.getPlatformEventLogger().logEvent("Core", "NOTICE", 
    //  " ------------ PLATFORM STARTED ----------- ");

    startHWThreads();
  }

  /**
   * @return the BuildingConfig associated with this HWModule
   */
  public BuildingConfig getBuildingConfig() {
    return building;
  }

  /**
   * @return the ProcessImage associated with this HWModule
   */
  public ProcessImage getProcessImage() {
    return procimg;
  }

  /**
   * Starts up all child HardwareInterfaces and maps sensors/actuators to their appropriate
   * interfaces.
   */
  private void startHWThreads() {
    ArrayList<HWInterfaceConfig> ifs=building.getHWInterfaces();
    for (HWInterfaceConfig hic:ifs) {
      try {
        HardwareInterface hif=hic.getInterfaceInstance(building);
        interfaces.put(hif.getName(), hif);
        boolean success=hif.startInterface(this, loggerManager,
            building.getSensorsWithInterface(hif.getName()),
            building.getActuatorsWithInterface(hif.getName()));
      } catch (Exception e) { //TODO: Error handling probably needs rework. startInterface() catches everything right now.
        System.out.println("Error: "+e.getMessage());
        //loggerManager.getPlatformEventLogger().logEvent(hic.getName(), "Initialization", "Error: "+e.getMessage());
      }
    }

    // Map sensors and actuators to their respective interface instances
    ArrayList<SensorConfig> sensors = building.getAllSensors();
    for (SensorConfig sc: sensors){
      deviceInterfaceMap.put(sc.getName(), interfaces.get(sc.getInterface().getName()));
    }
    ArrayList<ActuatorConfig> actuators = building.getAllActuators();
    for (ActuatorConfig ac : actuators){
      deviceInterfaceMap.put(ac.getName(), interfaces.get(ac.getInterface().getName()));
    }
  }

  /**
   * Ends the execution of all child HardwareInterfaces.
   */
  public void stopHWThreads() {
    for (HardwareInterface intf : interfaces.values()) {
      intf.stopInterface();
    }
  }

  /**
   * Fills the ProcessImage using the parsed SensorConfigs and ActuatorConfigs. Calling this method
   * will lock the ProcessImage.
   */
  private void fillProcessImage() {
    ArrayList<SensorConfig> srs=building.getAllSensors();
    for (SensorConfig sc:srs) {
      procimg.addSensor(sc);
    }
    ArrayList<ActuatorConfig> acs=building.getAllActuators();
    for (ActuatorConfig ac:acs) {
      procimg.addActuator(ac);
    }
    procimg.lockImage();
  }

  /**
   * Called by the processImage when an actuator setpoint has been changed
   * @param actuator the actuator whose setpoint has changed
   */
  public void handleActuation(Actuator actuator){
    // Lookup the HWInterface that should handle this actuation
    HardwareInterface intf = deviceInterfaceMap.get(actuator.getName());
    if (intf != null){
      intf.applyActuation(actuator);
    }
  }

  /**
   * Queries the appropriate HardwareInterface for a new measurement
   * @param sensor the sensor that should be read
   * @return a CompositeMeasurement of the reading, or null if no such polling is possible
   */
  public Double attemptRead(Sensor sensor){
    HardwareInterface intf = deviceInterfaceMap.get(sensor.getName());
    if (intf != null){
      return intf.read(sensor);
    }
    System.out.println("No interface matches sensor: "+sensor);
    //loggerManager.getPlatformEventLogger().logEvent("HWModule", "Error", "No interface matches sensor: "+sensor);
    return null;
  }

  /**
   * Propagate data to the ProcessImage
   * @param data a HashMap containing new measurements from a HardwareInterface
   */
  public void newData(HashMap<String, Double> data) {
    procimg.newData(data);
  }

}
