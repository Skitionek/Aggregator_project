package dtu.is31380;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import dtu.is31380.SystemConstants.DeviceType;

@SuppressWarnings("serial")
public class ActuatorConfig extends AbstractIOConfig {

  public static final String ACTUATOR_TAG="actuator";
  private static final String ELECTRICAL_TAG="electrical";
  private static final String STATE_ATTR="state";
  private static final String EL_P_ATTR="p";
  private static final String EL_PHASE_ATTR="phase";

  private double iniState;
  private Integer phase;
  private Double p_rated;

  public String toString() {
    StringBuffer rv=new StringBuffer("ActuatorConfig \""+name+"\"");
    return rv.toString();
  }

  public static ActuatorConfig parse(Node in) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (ACTUATOR_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();
      String an;
      String ref;
      double posx=0;
      double posy=0;
      boolean active;
      String at;
      String description = "";
      double iniState=0;

      Node _actuatorName=attrs.getNamedItem(NAME_ATTR);
      if (_actuatorName!=null) {
        an=_actuatorName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(ACTUATOR_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }

      Node _pos=attrs.getNamedItem(POS_ATTR);
      if (_pos!=null) {
        String posstr=_pos.getNodeValue();
        if (posstr.matches(POSPAIR_REGEX)) {
          String[] x12=posstr.split(",");
          posx=Double.valueOf(x12[0]);
          posy=Double.valueOf(x12[1]);
        }
      }
      else {
        throw new IllegalArgumentException(ACTUATOR_TAG+": Missing '"+POS_ATTR+"' attribute.");
      }

      Node _actuatorType=attrs.getNamedItem(TYPE_ATTR);
      if (_actuatorType!=null) {
        at=_actuatorType.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(ACTUATOR_TAG+": Missing '"+TYPE_ATTR+"' attribute.");
      }

      Node _referrer=attrs.getNamedItem(REF_ATTR);
      if (_referrer!=null) {
        ref=_referrer.getNodeValue();
      }
      else {
        ref=null;
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
        else throw new IllegalArgumentException(ACTUATOR_TAG+": '"+ACTIVE_ATTR+"' attribute '"+activestr+"' needs to be '0' or '1'.");
      }
      else {
        throw new IllegalArgumentException(ACTUATOR_TAG+": Missing '"+ACTIVE_ATTR+"' attribute.");
      }

      Node _iniState=attrs.getNamedItem(STATE_ATTR);
      if (_iniState!=null) {
        try {
          iniState=Double.valueOf(_iniState.getNodeValue()).doubleValue();
        }
        catch (NumberFormatException e) {
          throw new IllegalArgumentException(ACTUATOR_TAG+": '"+_iniState.getNodeValue()+"' is not a valid number.");
        }
      }
      else {
        throw new IllegalArgumentException(ACTUATOR_TAG+": Missing '"+STATE_ATTR+"' attribute.");
      }

      // A description is not required
      Node _description=attrs.getNamedItem(DESCRIPTION_ATTR);
      if (_description!=null) {
        description=_description.getTextContent();
      }


      try{
        DeviceType type = SystemConstants.getDeviceType(at);
        ActuatorConfig ac = new ActuatorConfig(an,posx,posy,type,active,iniState, ref, description);
        ac.parseInterface(in);
        ac.parseElectrical(in);
        return ac;
      }
      catch(IllegalArgumentException e){
        throw e;
      }
    }
    return null;
  }

  private void parseElectrical(Node n) {
    NodeList nl=n.getChildNodes();
    boolean found=false;
    for (int i=0;i<nl.getLength();i++) {
      Node in=nl.item(i);
      if ((in.getNodeType()==Node.ELEMENT_NODE) && (ELECTRICAL_TAG.equals(in.getNodeName()))) {
        if (found)
          throw new IllegalArgumentException("Only one '"+ELECTRICAL_TAG+"' allowed per actuator!");
        NamedNodeMap attrs=in.getAttributes();

        Node _prated=attrs.getNamedItem(EL_P_ATTR);
        if (_prated!=null) {
          try {
            p_rated=Double.valueOf(_prated.getNodeValue()).doubleValue();
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException(EL_P_ATTR+" for '"+name+"': Power value must be a valid number.");
          }
          found=true;
        }
        else {
          throw new IllegalArgumentException(ELECTRICAL_TAG+" for '"+name+"': Missing '"+EL_P_ATTR+"' attribute.");
        }
        
        Node _phase=attrs.getNamedItem(EL_PHASE_ATTR);
        if (_phase!=null) {
          String ph=_phase.getNodeValue().toLowerCase();
          if (ph.equals("x")) {
            phase=0;
          }
          else if (ph.equals("a") || ph.equals("r") || ph.equals("1")) {
            phase=1;
          }
          else if (ph.equals("b") || ph.equals("s") || ph.equals("2")) {
            phase=2;
          }
          else if (ph.equals("c") || ph.equals("t") || ph.equals("3")) {
            phase=3;
          }
          else {
            throw new IllegalArgumentException(EL_PHASE_ATTR+" for '"+name+"': Phase must be [a,b,c], [r,s,t], [0,1,2] or 'x' for unknown.");
          }
        }
        else {
          throw new IllegalArgumentException(ELECTRICAL_TAG+" for '"+name+"': Missing '"+EL_PHASE_ATTR+"' attribute.");
        }
      } //electrical node
    } //for i
  } //method
  
  private ActuatorConfig(String name,double posx,double posy,DeviceType type,boolean active,double iniState, String referrer, String description) {
    super(name,posx,posy,type,active, referrer, description);
    p_rated=null;
    phase=null;
    this.iniState=iniState;
  }

  public double getInitialState() {
    return iniState;
  }
  
  public double getRatedActivePowerConsumption() {
    if (p_rated==null) {
      throw new IllegalStateException("P_rated has not been configured.");
    }
    else return p_rated.doubleValue();
  }
  
}
