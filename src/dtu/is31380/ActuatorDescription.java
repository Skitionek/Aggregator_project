package dtu.is31380;

import java.io.Serializable;

public class ActuatorDescription extends EndpointDescription implements Serializable {

  private static final long serialVersionUID = -7737347905961994412L;

  public ActuatorDescription(String name, double posX, double posY, double posZ, String descriptiveText,
      String building, String room, String endpointType, String unit) {
    super(name, posX, posY, posZ, descriptiveText, building, room, endpointType, unit);
  }

  @Override
  public boolean isSensor() {
    return false;
  }

  @Override
  public boolean isActuator() {
    return true;
  }
}
