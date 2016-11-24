package dtu.is31380;

import java.io.Serializable;
import java.util.ArrayList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class EnvironmentConfig extends AbstractSpaceConfig implements Serializable {

  public static final String ENV_TAG="environment";
  private static final String NAME_ATTR="name";

  public String toString() {
    StringBuffer rv=new StringBuffer("EnvironmentConfig \""+name+"\" {\n");
    for (SensorConfig sc:sensors) {
      rv.append(sc.toString()+",");
    }
    for (ActuatorConfig ac:actuators) {
      rv.append(ac.toString()+",");
    }
    rv.append("}\n");
    return rv.toString();
  }
  
  public static EnvironmentConfig parse(Node in) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (ENV_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();
      String rn;
      
      Node _envName=attrs.getNamedItem(NAME_ATTR);
      if (_envName!=null) {
        rn=_envName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(ENV_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }
      EnvironmentConfig rc=new EnvironmentConfig(rn);
      
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
        } //ELEMENT_NODE found
      } //j walk through child nodes
      
      return rc;
    } //correct node
    return null;
  }
  
  private EnvironmentConfig(String name) {
    sensors=new ArrayList<SensorConfig>();
    actuators=new ArrayList<ActuatorConfig>();
    this.name=name;
  }
  
  public String getEnvironmentName() {
    return name;
  }
  
  public ArrayList<SensorConfig> getSensors() {
    return sensors;
  }
  
  public ArrayList<ActuatorConfig> getActuators() {
    return actuators;
  }
  
}


