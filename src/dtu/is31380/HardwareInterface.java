package dtu.is31380;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class HardwareInterface {
  private String ifName;
  private HWModule hwModule;
  private LoggerManager loggerManager;
  protected HashMap<String, Double> ifData;
  private String errorMessage;
  private boolean running;
  protected BuildingConfig buildingConfig;

  /**
   * Creates a new HardwareInterface using a supplied HWInterfaceConfig and optional BuildingConfig
   * @param hic the HWInterfaceConfig to use
   * @param buc optional parameter to pass the building configuration to the hardware driver if the driver has any need to
   *        get this information. If the driver does not need the building config, null may be passed here.
   */
  protected HardwareInterface(HWInterfaceConfig hic, BuildingConfig buc) {
    ifData = new HashMap<String,Double>();
    ifName = hic.getName();
    buildingConfig=buc;
    running=false;
  }

  public boolean isRunning() {
    return running;
  }
  
  public String getErrorMessage() {
    return errorMessage;
  }
  
  /**
   * Starts up this HardwareInterface. This method calls the startInterfaceImpl() function which handles startup
   * of the specific child interface. Should only be called from the HWModule.
   * @param module
   * @param loggerManager
   * @param sensors
   * @param actuators
   */
  public boolean startInterface(HWModule module, LoggerManager loggerManager, 
                                ArrayList<SensorConfig> sensors, ArrayList<ActuatorConfig> actuators) {
    hwModule = module;
    this.loggerManager = loggerManager;
    
    try {
      startInterfaceImpl(sensors, actuators);
      running=true;
      errorMessage="";
    } catch (Exception e) {
      System.out.println("Exception occured during startup of hardware interface. Message: "+e.getMessage());
      /*loggerManager.getPlatformEventLogger().logEvent(getName(), "ERROR", 
          "Exception occured during startup of hardware interface. Message: "+e.getMessage());*/
      running=false;
      errorMessage=e.getMessage();
      return false;
    }
    return true;
  }

  /**
   * Stops this particular HardwareInterface. Calls the stopInterfaceImpl() method of the actual interface.
   */
  //TODO Invalidate all data on stopping
  public void stopInterface() {
    try {
      stopInterfaceImpl();
      running=false;
      errorMessage="";
    } catch (Exception e) {
      System.out.println("Exception occured during shutdown of hardware interface. Message: "+e.getMessage());
      //loggerManager.getPlatformEventLogger().logEvent(getName(), "ERROR", 
      //    "Exception occured during shutdown of hardware interface. Message: "+e.getMessage());
      running=false;
      errorMessage=e.getMessage();
    }
  }

  /**
   * Called when this HardwareInterface is started. It is the job of the child interface to correctly map sensors
   * and actuators in a suitable way for internal use.
   * @param sensors all the sensors associated with this HardwareInterface
   * @param actuators all the actuators associated with this HardwareInterface
   */
  protected abstract void startInterfaceImpl(ArrayList<SensorConfig> sensors, ArrayList<ActuatorConfig> actuators);

  /**
   * Called when this HardwareInterface should stop. Use this method to clean up any threads/resources that are in use.
   */
  protected abstract void stopInterfaceImpl();

  /**
   * @return the name of this HardwareInterface
   */
  public String getName() {
    return ifName;
  }
  
  /**
   * @return a reference to the PlatformEventLogger
   */
  /*public PlatformEventLogger getLogger(){
    return loggerManager.getPlatformEventLogger();
  }*/

  /**
   * @return whether this interface has successfully connected
   */
  public abstract boolean isConnected();
  
  /**
   * Called when the HWModule requests an actuation on this interface
   * @param actuator for which the actuation must be executed
   */
  public abstract void applyActuation(Actuator actuator);
  
  /**
   * Updates the ProcessImage with all the data contained in the "mData" HashMap. Notifies the ControllerManager
   * of this update asynchronously. Calling this method will clear mData
   */
  protected final void notifyProcessImage(){
    HashMap<String, Double> data = ifData;
    ifData = new HashMap<String, Double>();
    if (hwModule!=null) { 
      hwModule.newData(data);
    }
    else {
      /*loggerManager.getPlatformEventLogger().logEvent(getName(), "WARNING", 
          "Trying to access HW module before interface start. This happens when a HardwareInterface subclass "+
          "tries to call notifyProcessImage() already in the constructor.");*/
    }
  }
  
  /**
   * Attempts a read of the specified sensor on this HardwareInterface. Returns null if not overridden by
   * a child class.
   * @param s the sensor to read
   * @return a Double representing the reading or null if no such reading can be done
   */
  public Double read(Sensor s){
    return null;
  }
}
