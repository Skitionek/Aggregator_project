package dtu.is31380;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import dtu.is31380.SystemConstants.DeviceType;

@SuppressWarnings("serial")
public class SensorConfig extends AbstractIOConfig {

  public static final String SENSOR_TAG="sensor";

  public String toString() {
    StringBuffer rv=new StringBuffer("SensorConfig \""+name+"\"");
    return rv.toString();
  }

  public static SensorConfig parse(Node in) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (SENSOR_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();
      String sn;
      String st;
      String ref;
      String description = "";
      double posx=0;
      double posy=0;
      boolean active=false;

      Node _sensorName=attrs.getNamedItem(NAME_ATTR);
      if (_sensorName!=null) {
        sn=_sensorName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(SENSOR_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }

      Node _pos=attrs.getNamedItem(POS_ATTR);
      if (_pos!=null) {
        String posstr=_pos.getNodeValue();
        if (posstr.matches(POSPAIR_REGEX)) {
          String[] x12=posstr.split(",");
          posx=Double.valueOf(x12[0]).doubleValue();
          posy=Double.valueOf(x12[1]).doubleValue();
        }
      }
      else {
        throw new IllegalArgumentException(SENSOR_TAG+": Missing '"+POS_ATTR+"' attribute.");
      }

      Node _referrer=attrs.getNamedItem(REF_ATTR);
      if (_referrer!=null) {
        ref=_referrer.getNodeValue();
      }
      else {
        ref=null;
      }
      
      Node _sensorType=attrs.getNamedItem(TYPE_ATTR);
      if (_sensorType!=null) {
        st=_sensorType.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(SENSOR_TAG+": Missing '"+TYPE_ATTR+"' attribute.");
      }

      Node _active=attrs.getNamedItem(ACTIVE_ATTR);
      if (_active!=null) {
        String activestr=_active.getNodeValue();
        if (activestr.equals("1") || activestr.equals("true") || activestr.equals("yes")) {
          active=true;
        }
        else if (activestr.equals("0") || activestr.equals("false") || activestr.equals("no")) {
          active=false;
        }
        else throw new IllegalArgumentException(SENSOR_TAG+": '"+ACTIVE_ATTR+"' attribute '"+activestr+"' needs to be '0' or '1'.");
      }
      else {
        throw new IllegalArgumentException(SENSOR_TAG+": Missing '"+ACTIVE_ATTR+"' attribute.");
      }

      // A description is not required
      Node _description=attrs.getNamedItem(DESCRIPTION_ATTR);
      if (_description!=null) {
        description=_description.getTextContent();
      }

      try{
        DeviceType type = SystemConstants.getDeviceType(st);
        SensorConfig sc=new SensorConfig(sn,posx,posy,type,active, ref, description);
        sc.parseInterface(in);
        return sc;
      }
      catch(IllegalArgumentException e){
        throw e;
      }
    }
    return null;
  }

  public SensorConfig(String name,double posx,double posy,DeviceType type,boolean active, String referrer, String description) {
    super(name,posx,posy,type,active, referrer, description);
  }
}

