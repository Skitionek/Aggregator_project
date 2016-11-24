package dtu.is31380;

import java.io.Serializable;

public class SensorDescription extends EndpointDescription implements Serializable {

  private static final long serialVersionUID = 5708932108181099686L;

  public SensorDescription(String name, double posX, double posY, double posZ, String descriptiveText, String building,
      String room, String endpointType, String unit) {
    super(name, posX, posY, posZ, descriptiveText, building, room, endpointType, unit);
  }

  @Override
  public boolean isSensor() {
    return true;
  }

  @Override
  public boolean isActuator() {
    return false;
  }
}
