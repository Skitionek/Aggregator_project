package dtu.is31380;

import java.util.ArrayList;

public class SystemConstants {

  public static final int DEFAULT_RMIPORT=1099;
  
  /**
   * Enum defining the type of devices in the flexhouse platform.
   * TODO: Expand to rest of flexhouse platform (Currently only applicable for flexhouse3)
   * TODO: More systematic classification (Is electrical power 'electrical' or 'power'?)
   *       This should probably be split into domain types and subtypes
   *       (e.g. 'Domain=electrical, quantity=voltage')
   */
  public static enum DeviceType{
    // Sensors
    Temperature("Temp", false),
    CO2("CO2", false),
    Humidity("Humidity", false),
    AnalogMeasurement("AnalogMeasurement", false),
    Motion("Motion", false),
    Presence("Presence", false),
    Brightness("Brightness", false),
    Window("Window", false),
    Door("Door", false),
    Voltage("Voltage", false),
    Current("Current", false),
    Power("Power", false),
    FlowTemperature("Flow Temperature", false),
    ReturnFlowTemperature("Return Flow Temperature", false),
    Flow("Flow", false),
    FlowPressure("Flow Pressure", false),
    Button("Button", false),
    WindDirection("Winddir", false),
    WindSpeed("Windspd", false),
    Insolation("Insolation", false),
    Electrical("Electrical", false),
    ActuatorState("ActuatorState", false),
    
    // Actuators
    PowerControlledHeater("PowerHeater", true),
    TemperatureControlledHeater("TempHeater", true),
    PowerControlledAirCon("PowerAirCon", true),
    TemperatureControlledAirCon("TempAirCon", true),
    Switch("Switch", true),
    Waterboiler("Waterboiler", true),
    DimmableLight("DimmableLight", true),
    Fridge("Fridge", true);
    
    
    // The tag that an XML file should supply to mark a device as this type
    public final String tag;
    
    // A type indicating either a sensor(0) or an actuator(1)
    public final boolean isActuator;
    
    DeviceType(String tag, boolean isActuator){
      this.tag = tag;
      this.isActuator = isActuator;
    }
  }

  
  /**
   * Method to get a devicetype from a tag specified in an XML file
   * @param tag for which a devicetype must be found
   * @return a devicetype
   * @throws Exception if the tag does not match any devicetype
   */
  public static DeviceType getDeviceType(String typetag) throws IllegalArgumentException{
    for (DeviceType type : DeviceType.values()){
      if (type.tag.equals(typetag)){
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid device type with tag: "+typetag);
  }

  /**
   * @return a list of all supported sensor types
   */
  public static DeviceType[] getSupportedSensors(){
    ArrayList<DeviceType> types = new ArrayList<DeviceType>();
    for (DeviceType type : DeviceType.values()){
      // Check for actuator
      if (!type.isActuator){
        types.add(type);
      }
    }
    return (DeviceType[]) types.toArray();
  }
  
  /**
   * @return a list of all supported actuator types
   */
  public static DeviceType[] getSupportedActuators(){
    ArrayList<DeviceType> types = new ArrayList<DeviceType>();
    for (DeviceType type : DeviceType.values()){
      // Check for actuator
      if (type.isActuator){
        types.add(type);
      }
    }
    return (DeviceType[]) types.toArray();
  }
}

