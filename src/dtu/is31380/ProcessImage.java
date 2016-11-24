package dtu.is31380;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import dtu.is31380.SystemConstants.DeviceType;

public class ProcessImage implements HouseControllerInterface {
  private Map<String, Sensor> sensors;
  private Map<String,Actuator> actuators;
  private HWModule hwModule;
  private boolean locked;
  private long simtimeref;

  // Concurrent Operations
  public static final int THREAD_COUNT = 1;
  private final ExecutorService mExecutor = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

  /**
   * Creates a new ProcessImage based on the data contained in a HWModule
   * @param module the HWModule containing the data for use with this ProcessImage
   */
  public ProcessImage(HWModule module, long simtimeref) {
    sensors=new HashMap<String,Sensor>();
    actuators=new HashMap<String,Actuator>();
    hwModule = module;
    locked=false;
    this.simtimeref=simtimeref;
  }

  /**
   * Shuts down the ProcessImage. Should be called in order to halt execution of the associated thread pool gracefully.
   */
  public void shutdown(){
    mExecutor.shutdownNow();
  }

  /**
   * Locks the ProcessImage by creating immutable copies of all added sensors and actuators.
   */
  public void lockImage() {
    locked=true;

    // Create defensive immutable copies of the sensor and actuator lists
    sensors = Collections.unmodifiableMap(sensors);
    actuators = Collections.unmodifiableMap(actuators);
  }

  /**
   * Adds a new Sensor to the ProcessImage based on a SensorConfig
   * @param sc the SensorConfig from which a Sensor should be instantiated
   */
  public void addSensor(SensorConfig sc) {
    if (locked) {
      throw new UnsupportedOperationException("Process image is locked. No new sensors can be added.");
    }
    if (sensors.containsKey(sc.getName())) {
      throw new IllegalArgumentException("Process image already contains a sensor named '"+sc.getName()+"'.");
    }
    sensors.put(sc.getName(),new Sensor(sc));
  }

  /**
   * Adds a new Actuator to the ProcessImage based on an ActuatorConfig
   * @param sc the ActuatorConfig from which a Actuator should be instantiated
   */
  public void addActuator(ActuatorConfig ac) {
    if (locked) {
      throw new UnsupportedOperationException("Process image is locked. No new sensors can be added.");
    }
    if (actuators.containsKey(ac.getName())) {
      throw new IllegalArgumentException("Process image already contains an actuator named '"+ac.getName()+"'.");
    }
    actuators.put(ac.getName(),new Actuator(ac));
  }

  /**
   * Updates the value of a given Sensor in the ProcessImage. Notifies the ControllerManager of this
   * update asynchronously.
   * @param name the name of the Sensor to update
   * @param value a CompositeMeasurement representing the last measurement received from this Sensor
   */
  private void updateSensor(String name, Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Null was passed as a value for sensor: "+name);
    }
    final Sensor s=sensors.get(name);
    if (s==null) {
      throw new IllegalArgumentException("No sensor named '"+name+"' in process image.");
    } 
    s.update(value);

  }

  /**
   * Updates the value of a given Actuator in the ProcessImage. Notifies the ControllerManager of this
   * update asynchronously.
   * @param name the name of the Actuator to update
   * @param value a CompositeMeasurement representing the last received actuation value
   */
  
  //TODO: This doesn't get called from anywhere right now!
  public void updateActuator(String name, Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Null was passed as a CompositeMeasurement for actuator: "+name);
    }
    final Actuator a=actuators.get(name);
    if (a==null) {
      throw new IllegalArgumentException("No actuator named '"+name+"' in process image.");
    }
    a.updateValue(value);
  }

  /**
   * Updates the setpoint of a given Actuator in the ProcessImage. Notifies the HWModule of this
   * update asynchronously.
   * @param name the name of the Actuator to update
   * @param value a Double representing the last specified setpoint
   */
  public void setActuator(String name, Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Null was passed as a setpoint for actuator: "+name);
    }

    final Actuator a=actuators.get(name);
    if (a==null) {
      throw new IllegalArgumentException("No actuator named '"+name+"' in process image.");
    }
    a.updateSetpoint(value);

    // Run asynchronously on a thread from the pool of worker threads
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        // Pass the update instruction to the HWModule
        try {
          hwModule.handleActuation(a);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    if (!mExecutor.isShutdown()){
      mExecutor.execute(runnable);
    }
  }

  /**
   * Updates the ProcessImage with all the data contained in the passed HashMap. This function is called by
   * HardwareInterfaces via the HWModule. Notifies the ControllerManager of this update asynchronously.
   * @param data a HashMap containing device names as keys and CompositeMeasurements as values.
   */
  public void newData(HashMap<String, Double> data) {
    for (String sn:data.keySet()) {
      try {
        if (sensors.containsKey(sn)) {
          updateSensor(sn,data.get(sn));
        }
        else if (actuators.containsKey(sn)){
          updateActuator(sn, data.get(sn));
        }
        else {
          System.out.println("Warning: No sensor or actuator named '"+sn+"' in process image. This should never happen.");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // Utility Methods

  public DeviceType getActuatorType(String name){
    if (name != null){
      Actuator a = actuators.get(name);
      if (a != null){
        return a.getType();
      }
    }
    return null;
  }

  public Double getSensorValue(String name) {
    if (name != null){
      Sensor s = sensors.get(name);

      if (s != null){
        // DISABLED FOR NOW - Easily causes overload of KNX Interface


        // Try polling this device using its HardwareInterface
        //CompositeMeasurement meas = hwModule.attemptRead(s);
        //if (meas != null){
        //  return meas;
        //}

        // Polling was not possible - return the last stored value
        return s.getValue();
      }
    }
    return null;
  }

  public Double getActuatorValue(String name) {
    if (name != null){
      Actuator a = actuators.get(name);
      if (a != null){
        return a.getValue();
      }
    }
    return null;
  }

  public Double getActuatorSetpoint(String name) {
    if (name != null){
      Actuator a = actuators.get(name);
      if (a != null){
        return a.getSetpoint();
      }
    }
    return null;
  }

  /**
   * Convenience methods
   */

  public Sensor[] getSensors() {
    return sensors.values().toArray(new Sensor[0]);
  }

  public Actuator[] getActuators() {
    return actuators.values().toArray(new Actuator[0]);
  }

  public Sensor getSensorByName(String name) {
    return sensors.get(name);
  }

  public Actuator getActuatorByName(String name) {
    return actuators.get(name);
  }

  @Override
  public Double getSimulationTime() {
    return new Double((System.currentTimeMillis()-simtimeref)/1000);
  }
  
  public BuildingConfig getBuildingConfig() {
    return hwModule.getBuildingConfig();
  }
}
