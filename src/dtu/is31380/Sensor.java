package dtu.is31380;

import java.io.Serializable;
import dtu.is31380.SystemConstants.DeviceType;

@SuppressWarnings("serial")
public class Sensor implements Serializable{
  private String name;
  private DeviceType type;
  
  private Double value;
  
  private static final String unit="-"; //TODO: Implement
  
  public Sensor(SensorConfig sc) {
    name=sc.getName();
    type=sc.getType();
    value=Double.NaN;
  }
  
  public String getName() {
    return name;
  }

  public DeviceType getType() {
    return type;
  }
  
  public String getUnit() {
    return unit;
  }
  
  public void update(Double v) {
    value=v;
  }
  
  public Double getValue() {
    return value;
  }

}
