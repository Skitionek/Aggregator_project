package dtu.is31380;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.io.Serializable;
import java.util.ArrayList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class RoomConfig extends AbstractSpaceConfig implements Serializable {
  
  public static final String ROOM_TAG="room";
  private static final String NAME_ATTR="name";
  private static final String FLOOR_ATTR="floor";
  private static final String CEIL_ATTR="ceiling";
  private static final String POLY_ATTR="polygon";
  private static final String OWALL_ATTR="outerwallarea";
  private static final String IWALL_ATTR="innerwallarea";
  private static final String WINDOW_ATTR="windowarea";
  private static final String POLYPAIR_REGEX = "-{0,1}[0-9]+\\.{0,1}[0-9]*,-{0,1}[0-9]+\\.{0,1}[0-9]*";

  private double floor;
  private double ceiling;
  private Shape poly;
  private double outerWallArea;
  private double innerWallArea;
  private double outerWindowArea;
  private double floorArea;
  private double roomHeight;
  private ArrayList<WindowConfig> windows; //windows associated with the room
  private ArrayList<DoorConfig> doors; //doors associated with the room
  
  public String toString() {
    StringBuffer rv=new StringBuffer("RoomConfig \""+name+"\" {\n");
    for (SensorConfig sc:sensors) {
      rv.append(sc.toString()+",");
    }
    for (ActuatorConfig ac:actuators) {
      rv.append(ac.toString()+",");
    }
    for (WindowConfig wc:windows) {
      rv.append(wc.toString()+",");
    }
    for (DoorConfig dc:doors) {
      rv.append(dc.toString()+",");
    }
    rv.append("}\n");
    return rv.toString();
  }
  
  public static RoomConfig parse(Node in) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (ROOM_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();
      String rn;
      double floor;
      double ceil;
      double owall=Double.NaN;
      double iwall=Double.NaN;
      double window=Double.NaN;
      ArrayList<Double> polyX=new ArrayList<Double>();
      ArrayList<Double> polyY=new ArrayList<Double>();
      
      Node _roomName=attrs.getNamedItem(NAME_ATTR);
      if (_roomName!=null) {
        rn=_roomName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(ROOM_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }
      
      Node _floor=attrs.getNamedItem(FLOOR_ATTR);
      if (_floor!=null) {
        try {
          floor=Double.valueOf(_floor.getNodeValue());
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ROOM_TAG+": '"+FLOOR_ATTR+"' attribute needs a numeric argument.");
        }
      }
      else {
        throw new IllegalArgumentException(ROOM_TAG+": Missing '"+FLOOR_ATTR+"' attribute.");
      }

      Node _ceil=attrs.getNamedItem(CEIL_ATTR);
      if (_ceil!=null) {
        try {
          ceil=Double.valueOf(_ceil.getNodeValue());
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ROOM_TAG+": '"+CEIL_ATTR+"' attribute needs a numeric argument.");
        }
      }
      else {
        throw new IllegalArgumentException(ROOM_TAG+": Missing '"+CEIL_ATTR+"' attribute.");
      }
      
      Node _poly=attrs.getNamedItem(POLY_ATTR);
      if (_poly!=null) {
        String polystr=_poly.getNodeValue();
        String[] polypairs=polystr.split(" ");
        for (String pair:polypairs) {
          if (!pair.matches(POLYPAIR_REGEX)) {
            String[] x12=pair.split(POLYPAIR_REGEX);
            polyX.add(Double.valueOf(x12[0]).doubleValue());
            polyY.add(Double.valueOf(x12[1]).doubleValue());
          }
        }
      }
      else {
        throw new IllegalArgumentException(ROOM_TAG+": Missing '"+POLY_ATTR+"' attribute.");
      }
      double[] dx=new double[polyX.size()];
      double[] dy=new double[polyY.size()];
      for (int i=0;i<polyX.size();i++) {
        dx[i]=polyX.get(i);
        dy[i]=polyY.get(i);
      }
      
      Node _owall=attrs.getNamedItem(OWALL_ATTR);
      if (_owall!=null) {
        try {
          owall=Double.valueOf(_owall.getNodeValue());
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ROOM_TAG+": '"+OWALL_ATTR+"' attribute needs a numeric argument.");
        }
      }

      Node _iwall=attrs.getNamedItem(IWALL_ATTR);
      if (_iwall!=null) {
        try {
          iwall=Double.valueOf(_iwall.getNodeValue());
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ROOM_TAG+": '"+IWALL_ATTR+"' attribute needs a numeric argument.");
        }
      }
      
      Node _window=attrs.getNamedItem(WINDOW_ATTR);
      if (_window!=null) {
        try {
          window=Double.valueOf(_window.getNodeValue());
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ROOM_TAG+": '"+WINDOW_ATTR+"' attribute needs a numeric argument.");
        }
      }
      
      RoomConfig rc=new RoomConfig(rn,floor,ceil,dx,dy,owall,iwall,window);
      
      NodeList instruN=in.getChildNodes();
      for (int j=0;j<instruN.getLength();j++) {
        Node xn=instruN.item(j);
        if (xn.getNodeType()==Node.ELEMENT_NODE) {
          String nonm=xn.getNodeName();
          if (SensorConfig.SENSOR_TAG.equals(nonm)) {
            SensorConfig sc=SensorConfig.parse(xn);
            rc.addSensorConfig(sc);
          }
          else if (ActuatorConfig.ACTUATOR_TAG.equals(nonm)) {
            ActuatorConfig ac=ActuatorConfig.parse(xn);
            rc.addActuatorConfig(ac);
          }
          else if (WindowConfig.WINDOW_TAG.equals(nonm)) {
            WindowConfig wc=WindowConfig.parse(xn,rc);
            rc.addWindowConfig(wc);
          }
          else if (DoorConfig.DOOR_TAG.equals(nonm)) {
            DoorConfig dc=DoorConfig.parse(xn,rc);
            rc.addDoorConfig(dc);
          }
        } //ELEMENT_NODE found
      } //j walk through child nodes
      
      
      rc.linkSensors();
      return rc;
    } //correct node
    return null;
  }

  private RoomConfig(String name, double floor, double ceiling, double[] polyX, double[] polyY,
                     double owall, double iwall, double window) {
    sensors=new ArrayList<SensorConfig>();
    actuators=new ArrayList<ActuatorConfig>();
    windows=new ArrayList<WindowConfig>();
    doors=new ArrayList<DoorConfig>();
    this.name=name;
    this.floor=floor;
    this.ceiling=ceiling;
    this.outerWallArea=owall;
    this.innerWallArea=iwall;
    this.outerWindowArea=window;
    GeneralPath gp=new GeneralPath();
    if (polyX.length>1) {
      gp.moveTo(polyX[0], polyY[0]);
      for (int i=1;i<polyX.length;i++) {
        gp.lineTo(polyX[i],polyY[i]);
      }
      gp.lineTo(polyX[0],polyY[0]);
    }
    poly=gp;
    roomHeight=ceiling-floor;
    floorArea=calculatePolygonArea(polyX,polyY);
  }
  
  /**
   * Creates a link between sensors/actuators and the room in which they are situated.
   */
  private void linkSensors(){
    for (SensorConfig sensor : sensors){
      sensor.setRoom(this);
      if (sensor.type == SystemConstants.DeviceType.Door) {
        String ref=sensor.getReferrer();
        for (DoorConfig dc:doors) {
          if (ref.equals(dc.name)) {
            dc.addSensorConfig(sensor);
          }
        }
      }
      else if (sensor.type == SystemConstants.DeviceType.Window) {
        String ref=sensor.getReferrer();
        for (WindowConfig wc:windows) {
          if (ref.equals(wc.name)) {
            wc.addSensorConfig(sensor);
          }
        }
      }
    }
    for (ActuatorConfig actuator : actuators){
      actuator.setRoom(this);
      if (actuator.type == SystemConstants.DeviceType.Door) {
        String ref=actuator.getReferrer();
        for (DoorConfig dc:doors) {
          if (ref.equals(dc.name)) {
            dc.addActuatorConfig(actuator);
          }
        }
      }
      else if (actuator.type == SystemConstants.DeviceType.Window) {
        String ref=actuator.getReferrer();
        for (WindowConfig wc:windows) {
          if (ref.equals(wc.name)) {
            wc.addActuatorConfig(actuator);
          }
        }
      }
    }
  }

  private double calculatePolygonArea(double[] X, double[] Y) { 
    double area = 0;         // Accumulates area in the loop
    int j = X.length-1;  // The last vertex is the 'previous' one to the first

    for (int i=0; i<X.length; i++)
      { area = area +  (X[j]+X[i]) * (Y[j]-Y[i]); 
        j = i;  //j is previous vertex to i
      }
    return area/2;
  }
  
  
  
  private void addWindowConfig(WindowConfig wc) {
    windows.add(wc);
  }
  
  private void addDoorConfig(DoorConfig dc) {
    doors.add(dc);
  }
  
  public String getRoomName() {
    return name;
  }
  
  public double getFloorAltitude() {
    return floor;
  }
 
  public double getCeilingAltitude() {
    return ceiling;
  }
 
  public Shape getRoomPolygon() {
    return poly;
  }

  public double getRoomHeight() {
    return roomHeight;
  }
  
  public double getOuterWallArea() {
    if (outerWallArea==Double.NaN) {
      throw new IllegalStateException("Outer wall area has not been defined in configuration file.");
    }
    return outerWallArea;
  }
  
  public double getInnerWallArea() {
    if (innerWallArea==Double.NaN) {
      throw new IllegalStateException("Inner wall area has not been defined in configuration file.");
    }
    return innerWallArea;
  }
  
  public double getOuterWindowArea() {
    if (outerWindowArea==Double.NaN) {
      throw new IllegalStateException("Outer window area has not been defined in configuration file.");
    }
    return outerWindowArea;
  }
  
  public double getFloorArea() {
    return floorArea;
  }
  
  public ArrayList<DoorConfig> getDoors() {
    return doors;
  }
  
  public ArrayList<WindowConfig> getWindows() {
    return windows;
  }
  
}
