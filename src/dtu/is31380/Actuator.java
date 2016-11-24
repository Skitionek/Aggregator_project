package dtu.is31380;

import java.io.Serializable;
import dtu.is31380.SystemConstants.DeviceType;

@SuppressWarnings("serial")
public class Actuator implements Serializable{
  private String name;
  private DeviceType type;
  
  private Double setpoint;
  private Double value;
  
  private static final String unit="-"; //TODO: Implement
  
  public Actuator(ActuatorConfig sc) {
    name=sc.getName();
    type=sc.getType();
    setpoint=Double.NaN;
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
  
  public void updateSetpoint(Double s) {
    setpoint=s;
  }

  public void updateValue(Double v) {
    value=v;
  }
  
  public boolean inEffect() {
    return (value==setpoint);
  }
  
  public Double getSetpoint() {
    return setpoint;
  }
  
  public Double getValue() {
    return value;
  }
  
}

