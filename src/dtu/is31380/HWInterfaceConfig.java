package dtu.is31380;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class HWInterfaceConfig {

  static class HWSubInterface {
    public String tag;
    public Class<? extends HWInterfaceConfig> parser;

    public HWSubInterface(String tag,Class<? extends HWInterfaceConfig> parser) {
      this.tag=tag;
      this.parser=parser;
    }
  }

  public static final String HWINTERFACE_TAG="hwinterface";
  private static final String NAME_ATTR="name";
  private static final String TYPE_ATTR="type";
  private static final String CONFIG_ATTR="config";

  private String name;
  protected ArrayList<SensorConfig> sensors;
  protected ArrayList<ActuatorConfig> actuators;
  
  @SuppressWarnings("unchecked")
  public static HWSubInterface loadInterface(String identifier){
    // Get the correct hardware interface
    try {
      Class<? extends HWInterfaceConfig> aClass = (Class<? extends HWInterfaceConfig>) 
          Class.forName("dtu.is31380."+identifier);

      // Get the type field
      Field field = aClass.getDeclaredField("TYPESTRING");
      String typeString = (String) field.get(null);

      return new HWSubInterface(typeString, aClass);
    } catch (ClassNotFoundException e) {
      System.out.println("Error: "+e);
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    // Should never happen
    return null;
  }

  public String toString() {
    StringBuilder rv=new StringBuilder("HWInterfaceConfig \""+name+"\", type=\""+getType()+"\"");
    toStringImpl(rv);
    return rv.toString();
  }

  protected HWInterfaceConfig() {
    sensors=new ArrayList<SensorConfig>();
    actuators=new ArrayList<ActuatorConfig>();
  }

  protected abstract void toStringImpl(StringBuilder b);

  public static HWInterfaceConfig parse(Node in) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (HWINTERFACE_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();

      String ifname;
      Node _ifName=attrs.getNamedItem(NAME_ATTR);
      if (_ifName!=null) {
        ifname=_ifName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }

      String iftype;
      Node _ifType=attrs.getNamedItem(TYPE_ATTR);
      if (_ifType!=null) {
        iftype=_ifType.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+TYPE_ATTR+"' attribute.");
      }

      String ifconfig;
      Node _ifConfig=attrs.getNamedItem(CONFIG_ATTR);
      if (_ifConfig!=null) {
        ifconfig=_ifConfig.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+CONFIG_ATTR+"' attribute.");
      }

      //    the config class dynamically
      HWSubInterface hsi = loadInterface(ifconfig);
      if (hsi.tag.equals(iftype)) {
        try {
          HWInterfaceConfig rv=hsi.parser.newInstance();
          rv.parseSubInterface(attrs);
          rv.name=ifname;
          return rv;
        }
        catch (InstantiationException e) {
          System.out.println("HW Interface config class '"+hsi.parser.getCanonicalName()+
              "' cannot be instantiated. Maybe no public no-arg constructor?");
          e.printStackTrace();
        }
        catch (IllegalAccessException e) {
          System.out.println("HW Interface config class '"+hsi.parser.getCanonicalName()+
              "' cannot be instantiated. Maybe no public no-arg constructor?");
          e.printStackTrace();
        }
      }
      System.out.println("No HW Interface config class of type '"+iftype+"' found. Cannot instantiate.");
    }
    return null;
  }

  protected abstract void parseSubInterface(NamedNodeMap attrs);

  public abstract String getType();
  
  public abstract String getAddress();

  public abstract HardwareInterface getInterfaceInstance(BuildingConfig buc);

  public String getName() {
    return name;
  }

  public void addSensor(SensorConfig sc) {
    sensors.add(sc);
  }

  public void addActuator(ActuatorConfig ac) {
    actuators.add(ac);
  }

  public ArrayList<SensorConfig> getSensors() {
    return sensors;
  }

  public ArrayList<ActuatorConfig> getActuators() {
    return actuators;
  }

}
