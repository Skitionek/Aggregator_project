package dtu.is31380;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class BuildingConfig implements Serializable {

  private static final String BUILDING_TAG="building";
  private static final String NAME_ATTR="name";
  private static final String DESC_ATTR="description";
  private static final String LON_ATTR="longitude";
  private static final String LAT_ATTR="latitude";
  private static final String ALT_ATTR="altitude";

  private static DocumentBuilderFactory myParserFactory;
  private Document cFile;
  
  public int sensorAmount = 0;
  public int actuatorAmount = 0;
  
  private String buildingName;
  private String buildingDesc;
  private double latitude;
  private double longitude;
  private double altitude;
  private ArrayList<RoomConfig> rooms;
  private ArrayList<SensorConfig> sensors; //sensors associated with the building itself, not one of the rooms
  private ArrayList<ActuatorConfig> actuators; //actuators associated with the building itself, not one of the rooms
  private ArrayList<HWInterfaceConfig> interfaces;
  private EnvironmentConfig env;
  private ArrayList<EndpointDescription> descriptions;
  
  static {
    myParserFactory=DocumentBuilderFactory.newInstance();
  }

  public BuildingConfig() {
    rooms=new ArrayList<RoomConfig>();
    sensors=new ArrayList<SensorConfig>();
    actuators=new ArrayList<ActuatorConfig>();
    interfaces=new ArrayList<HWInterfaceConfig>();
    env=null;
  }

  public String toString() {
    StringBuffer rv=new StringBuffer("BuildingConfig {\n");
    for (HWInterfaceConfig hc:interfaces) {
      rv.append(hc.toString()+",\n");
    }
    for (RoomConfig rc:rooms) {
      rv.append(rc.toString()+",");
    }
    for (SensorConfig sc:sensors) {
      rv.append(sc.toString()+",");
    }
    for (ActuatorConfig ac:actuators) {
      rv.append(ac.toString()+",");
    }
    rv.append("}\n");
    return rv.toString();
  }
  
  //TODO Log these errors properly instead of crashing platform
  public void parseConfig(String configFile) {
    try {
      DocumentBuilder parse=myParserFactory.newDocumentBuilder();
      cFile=parse.parse(new File(configFile));
    }
    catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  
    if (cFile==null)
      throw new IllegalArgumentException("BuildingConfig: Configuration file '"+configFile+"' could not be read."); 
    
    //-------------------------parse the first (building) level--------------------------------------

    NodeList bldg=cFile.getElementsByTagName(BUILDING_TAG);
    if ((bldg==null) || (bldg.getLength()!=1)) {
      throw new IllegalArgumentException("Configuration file must contain exactly one instance of '"+BUILDING_TAG+"'.");
    }
    Node n=bldg.item(0);
    NamedNodeMap attrs=n.getAttributes();

    Node _buildingName=attrs.getNamedItem(NAME_ATTR);
    if (_buildingName!=null) {
      buildingName=_buildingName.getNodeValue();
    }
    else {
      throw new IllegalArgumentException(BUILDING_TAG+": Missing '"+NAME_ATTR+"' attribute.");
    }
    
    Node _buildingDesc=attrs.getNamedItem(DESC_ATTR);
    if (_buildingDesc!=null) {
      buildingDesc=_buildingDesc.getNodeValue();
    }
    else {
      throw new IllegalArgumentException(BUILDING_TAG+": Missing '"+DESC_ATTR+"' attribute.");
    }
    
    Node _latitude=attrs.getNamedItem(LAT_ATTR);
    if (_latitude!=null) {
      try {
        latitude=Double.valueOf(_latitude.getNodeValue());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(BUILDING_TAG+": '"+LAT_ATTR+"' attribute needs a numeric argument.");
      }
    }
    else {
      throw new IllegalArgumentException(BUILDING_TAG+": Missing '"+LAT_ATTR+"' attribute.");
    }
    
    Node _longitude=attrs.getNamedItem(LON_ATTR);
    if (_longitude!=null) {
      try {
        longitude=Double.valueOf(_longitude.getNodeValue());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(BUILDING_TAG+": '"+LON_ATTR+"' attribute needs a numeric argument.");
      }
    }
    else {
      throw new IllegalArgumentException(BUILDING_TAG+": Missing '"+LON_ATTR+"' attribute.");
    }
    
    Node _altitude=attrs.getNamedItem(ALT_ATTR);
    if (_altitude!=null) {
      try {
        altitude=Double.valueOf(_altitude.getNodeValue());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(BUILDING_TAG+": '"+ALT_ATTR+"' attribute needs a numeric argument.");
      }
    }
    else {
      throw new IllegalArgumentException(BUILDING_TAG+": Missing '"+ALT_ATTR+"' attribute.");
    }
    
    

    //-------------------------parse the second level (rooms, sensors, actuators, environment)--------------------------------------
    
    NodeList instruN=n.getChildNodes();
    for (int j=0;j<instruN.getLength();j++) {
      Node in=instruN.item(j);
      if (in.getNodeType()==Node.ELEMENT_NODE) {
        String nonm=in.getNodeName();
        if (RoomConfig.ROOM_TAG.equals(nonm)) {
          RoomConfig rc=RoomConfig.parse(in);
          rooms.add(rc);
        }
        else if (EnvironmentConfig.ENV_TAG.equals(nonm)) {
          EnvironmentConfig ec=EnvironmentConfig.parse(in);
          if (env!=null) {
            throw new IllegalArgumentException("BuildingConfig: Only one '"+EnvironmentConfig.ENV_TAG+"' section per building possible.");
          }
          env=ec;
        }
        else if (SensorConfig.SENSOR_TAG.equals(nonm)) {
          SensorConfig sc=SensorConfig.parse(in);
          sensors.add(sc);
        }
        else if (ActuatorConfig.ACTUATOR_TAG.equals(nonm)) {
          ActuatorConfig ac=ActuatorConfig.parse(in);
          actuators.add(ac);
        }
        else if (HWInterfaceConfig.HWINTERFACE_TAG.equals(nonm)) {
          HWInterfaceConfig hic=HWInterfaceConfig.parse(in);
          if (hic!=null) {
            interfaces.add(hic);
          }
        }
      } //ELEMENT_NODE found
    } //j walk through child nodes
    linkSensors();
    linkSpaces();
    createDescriptions();
  } //parseConfig method
  
  /*
   * Creates object links between all sensors and actuators and their respective
   * hwinterfaces. 
   */
  private void linkSensors() {
    for (SensorConfig sc:getAllSensors()) {
      HWInterfaceConfig hic=null;
      for (HWInterfaceConfig h:interfaces) {
        if (h.getName().equals(sc.ifName)) {
          hic=h;
        }
      }
      if (hic==null) {
      // Dont bring the platform down. Simply log this error and continue boot
      System.out.println("Sensor '"+sc.name+"': No hwinterface named '"+sc.ifName+"'.");
      //logger.logEvent("BuildingConfig", "Parsing Error", "Sensor '"+sc.name+"': No hwinterface named '"+sc.ifName+"'.");
        //throw new IllegalArgumentException("Sensor '"+sc.name+"': No hwinterface named '"+sc.ifName+"'.");
      }
      else {
        sc.setHWInterface(hic);
        hic.addSensor(sc);
      }
    }
    for (ActuatorConfig ac:getAllActuators()) {
      HWInterfaceConfig hic=null;
      for (HWInterfaceConfig h:interfaces) {
        if (h.getName().equals(ac.ifName)) {
          hic=h;
        }
      }
      if (hic==null) {
      System.out.println("Actuator '"+ac.name+"': No hwinterface named '"+ac.ifName+"'.");
        //logger.logEvent("BuildingConfig", "Parsing Error", "Actuator '"+ac.name+"': No hwinterface named '"+ac.ifName+"'.");
        //throw new IllegalArgumentException("Actuator '"+ac.name+"': No hwinterface named '"+ac.ifName+"'.");
      }
      else {
        ac.setHWInterface(hic);
        hic.addActuator(ac);
      }
    }
  }
  
  /*
   * Creates object links between all doors and windows and the spaces they connect.
   */
  private void linkSpaces() {
    for (RoomConfig rc:rooms) {
      for (DoorConfig dc:rc.getDoors()) {
        boolean matched=false;
        for (RoomConfig match:rooms) {
          if (dc.matchSpace(match)) {
            matched=true;
            break;
          }
        }
        if (env!=null) {
          if (dc.matchSpace(env)) {
            matched=true;
          }
        }
        if (!matched) {
          System.out.println("Door '"+dc.getName()+"' does not link to a matching room.");
          //logger.logEvent("BuildingConfig", "Parsing Error", "Door '"+dc.getName()+"' does not link to a matching room.");
        }
      } //door matching loop
      
      for (WindowConfig wc:rc.getWindows()) {
        boolean matched=false;
        for (RoomConfig match:rooms) {
          if (wc.matchSpace(match)) {
            matched=true;
            break;
          }
        }
        if (env!=null) {
          if (wc.matchSpace(env)) {
            matched=true;
          }
        }
        if (!matched) {
          System.out.println("Window '"+wc.getName()+"' does not link to a matching room.");
          //logger.logEvent("BuildingConfig", "Parsing Error", "Window '"+wc.getName()+"' does not link to a matching room.");
        }
      } //window matching loop
      
    }
  }
  
  //TODO Finish implementation
  private void createDescriptions(){
    sensorAmount = getAllSensors().size();
    actuatorAmount = getAllActuators().size();

    descriptions = new ArrayList<EndpointDescription>();
    for (SensorConfig sensor : getAllSensors()){
      Point2D pos = sensor.getPosition();
      RoomConfig room = getRoomForSensor(sensor.getName());
      String placement = (room != null) ? room.getRoomName(): "N.A.";
      descriptions.add(new SensorDescription(sensor.getName(), pos.getX(), pos.getY(), 0.0, "test", 
          getBuildingName(), placement, sensor.getType().toString(), "testunit"));
    }
    for (ActuatorConfig actuator : getAllActuators()){
      Point2D pos = actuator.getPosition();
      RoomConfig room = getRoomForActuator(actuator.getName());
      String placement = (room != null) ? room.getRoomName(): "N.A.";
      descriptions.add(new ActuatorDescription(actuator.getName(), pos.getX(), pos.getY(), 0.0, "test", 
          getBuildingName(), placement, actuator.getType().toString(), "testunit"));
    }
  }
  
  public ArrayList<EndpointDescription> getEndpointDescriptions(){
    return descriptions;
  }
  
  public ArrayList<? extends EndpointDescription> getSensorDescriptions(){
      return new ArrayList<EndpointDescription>(getEndpointDescriptions().subList(0, sensorAmount));
  }
  
  public ArrayList<? extends EndpointDescription> getActuatorDescriptions(){
    return new ArrayList<EndpointDescription>(getEndpointDescriptions().subList(sensorAmount, sensorAmount+actuatorAmount));
  }
  
  public String getBuildingName() {
    return buildingName;
  }
  
  public String getBuildingDescription() {
    return buildingDesc;
  }
  
  public ArrayList<SensorConfig> getBuildingLevelSensors() {
  ArrayList<SensorConfig> rv=new ArrayList<SensorConfig>();
  rv.addAll(sensors);
  if (env!=null)
        rv.addAll(env.getSensors());
    return rv;
  }
  
  public ArrayList<ActuatorConfig> getBuildingLevelActuators() {
  ArrayList<ActuatorConfig> rv=new ArrayList<ActuatorConfig>();
  rv.addAll(actuators);
    if (env!=null)
      rv.addAll(env.getActuators());
  return rv;
  }
  
  public ArrayList<SensorConfig> getAllSensors() {
    ArrayList<SensorConfig> rv=new ArrayList<SensorConfig>();
    rv.addAll(sensors);
    for (RoomConfig rc:rooms) {
      rv.addAll(rc.getSensors());
    }
    if (env!=null)
      rv.addAll(env.getSensors());
    return rv;
  }
  
  public ArrayList<ActuatorConfig> getAllActuators() {
    ArrayList<ActuatorConfig> rv=new ArrayList<ActuatorConfig>();
    rv.addAll(actuators);
    for (RoomConfig rc:rooms) {
      rv.addAll(rc.getActuators());
    }
    if (env!=null)
      rv.addAll(env.getActuators());
    return rv;
  }
  
  public ArrayList<RoomConfig> getRooms() {
    return rooms;
  }

  public ArrayList<HWInterfaceConfig> getHWInterfaces() {
    return interfaces;
  }
  
  public ArrayList<SensorConfig> getSensorsWithInterface(String ifc) {
    ArrayList<SensorConfig> srs=getAllSensors();
    ArrayList<SensorConfig> rv=new ArrayList<SensorConfig>();
    for (SensorConfig s:srs) {
      if (s.getInterfaceName().equals(ifc))
        rv.add(s);
    }
    return rv;
  }

  public ArrayList<ActuatorConfig> getActuatorsWithInterface(String ifc) {
    ArrayList<ActuatorConfig> acs=getAllActuators();
    ArrayList<ActuatorConfig> rv=new ArrayList<ActuatorConfig>();
    for (ActuatorConfig a:acs) {
      if (a.getInterfaceName().equals(ifc))
        rv.add(a);
    }
    return rv;
  }
  
  public RoomConfig getRoomForSensor(String sname) {
    for (RoomConfig rc:rooms) {
      for (SensorConfig sc:rc.getSensors()) {
        if (sc.getName().equals(sname)) {
          return rc;
        }
      }
    }
    return null;
  }
  
  public RoomConfig getRoomForActuator(String aname) {
    for (RoomConfig rc:rooms) {
      for (ActuatorConfig ac:rc.getActuators()) {
        if (ac.getName().equals(aname)) {
          return rc;
        }
      }
    }
    return null;
  }

  public ArrayList<ActuatorConfig> getAssociatedActuators(String sensor) {
    // TODO Auto-generated method stub
    return null;
  }
  
  public double getLongitude(){
    return longitude;
  }
  
  public double getLatitude(){
    return latitude;
  }
  
  public double getAltitude(){
    return altitude;
  }
  
} //class

