package dtu.is31380;

import java.io.Serializable;

public abstract class EndpointDescription implements Serializable {

  private static final long serialVersionUID = 3451670654415879115L;

  protected String name;
  protected double posX;
  protected double posY;
  protected double posZ;
  protected String descriptiveText;
  protected String building;
  protected String room;
  protected String endpointType;
  protected String unit;

  protected EndpointDescription() {
  }

  protected EndpointDescription(String name, double posX, double posY, double posZ, String descriptiveText,
      String building, String room, String endpointType, String unit) {
    this.name = name;
    this.posX = posX;
    this.posY = posY;
    this.posZ = posZ;
    this.descriptiveText = descriptiveText;
    this.building = building;
    this.room = room;
    this.endpointType = endpointType;
    this.unit = unit;
  }

  public abstract boolean isSensor();

  public abstract boolean isActuator();

  public String getName() {
    return name;
  }

}
