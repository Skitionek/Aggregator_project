package dtu.is31380;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import dtu.is31380.SystemConstants.DeviceType;

@SuppressWarnings("serial")
public abstract class AbstractIOConfig implements Serializable{

  protected static final String NAME_ATTR="name";
  protected static final String TYPE_ATTR="type";
  protected static final String POS_ATTR="pos";
  protected static final String REF_ATTR="ref";
  protected static final String ACTIVE_ATTR="active";
  protected static final String DESCRIPTION_ATTR="description";
  protected static final String POSPAIR_REGEX = "-{0,1}[0-9]+\\.{0,1}[0-9]*,-{0,1}[0-9]+\\.{0,1}[0-9]*";
  private static final String INTERFACE_TAG = "interface";
  
  protected String name;
  protected Point2D.Double position;
  protected DeviceType type;
  protected boolean active;
  protected String description;
  protected String referrer;
  protected String ifName;                     //Name of the hardware interface connected to this sensor/actuator 
  protected HashMap<String,String> ifAttrs;  //HashMap with (interface specific) parsed attributes for this sensor/actuator
  private HWInterfaceConfig ifLink;            //Object reference to the hardware interface config
  private RoomConfig room;                     //Object reference to the room config in which this sensor/actuator is located
                                               //TODO: What do we do with sensors which are not associated with a room?
                                               //      They could either be part of the environment or the building.
  
  protected AbstractIOConfig(String name,double posx,double posy,DeviceType type,boolean active, String referrer,String description) {
    this.name=name;
    this.position=new Point2D.Double(posx,posy);
    this.type=type;
    this.active=active;
    this.description = description;
    this.referrer=referrer;
    ifAttrs=new HashMap<String,String>();
    ifLink=null;
    room=null;
  }
  
  public String getName() {
    return name;
  }
  
  public String getReferrer() {
    return referrer;
  }
  
  public boolean getStatus() {
    return active;
  }
  
  public String getDescription() {
    return description;
  }
 
  public HashMap<String,String> getInterfaceAttributes() {
    return ifAttrs;
  }
  
  public String getInterfaceName() {
    return ifName;
  }
  
  public void setHWInterface(HWInterfaceConfig h) {
    ifLink=h;
  }
  
  public void setRoom(RoomConfig room){
    this.room = room;
  }
  
  public RoomConfig getRoom() {
    return room;
  }
  
  public HWInterfaceConfig getInterface() {
    return ifLink;
  }
  
  
  public DeviceType getType() {
    return type;
  }
  
  public Point2D getPosition(){
  return position;
  }
  
  /**
   * @return returns all aliases for this particular key (i.e. links to more than one address).
   */
  public String[] getAliases(String key){
    String mainAttribute = ifAttrs.get(key);
    if (mainAttribute != null){
      ArrayList<String> aliases = new ArrayList<String>();
      // Lookup all similar keys that are not directly equal to the key
      for (String keyInSet : ifAttrs.keySet()){
        if (keyInSet.contains(key) && !keyInSet.equals(key)){
          aliases.add(keyInSet);
        }
      }
      // All aliases have now been tested - return as a String array
      if (!aliases.isEmpty()){
        String[] array = new String[aliases.size()];
        aliases.toArray(array);
        return array;
      }
    }
    return null;
  }
  
  protected void parseInterface(Node n) {
    NodeList nl=n.getChildNodes();
    boolean found=false;
    for (int i=0;i<nl.getLength();i++) {
      Node in=nl.item(i);
      if ((in.getNodeType()==Node.ELEMENT_NODE) && (INTERFACE_TAG.equals(in.getNodeName()))) {
        if (found)
          throw new IllegalArgumentException("Only one '"+INTERFACE_TAG+"' allowed per sensor/actuator!");
        NamedNodeMap attrs=in.getAttributes();

        Node _iName=attrs.getNamedItem(NAME_ATTR);
        if (_iName!=null) {
          ifName=_iName.getNodeValue();
          found=true;
        }
        else {
          throw new IllegalArgumentException(INTERFACE_TAG+" for '"+name+"': Missing '"+NAME_ATTR+"' attribute.");
        }
        
        for (int j=0;j<attrs.getLength();j++) {
          Node ta=attrs.item(j);
          if (!ta.getNodeName().equals(NAME_ATTR)) {
            if (ta.getNodeValue().contains("/")){
              // This attribute has multiple parameters 
              String[] aliases = ta.getNodeValue().split("/");
              
              // Save the main alias as "nodeName"
              ifAttrs.put(ta.getNodeName(), aliases[0]);
              
              // Save aliases as "nodeName.alias1, nodename.alias2 ..."
              for (int k=1; k<aliases.length; k++){
                ifAttrs.put(ta.getNodeName()+"alias"+k, aliases[k]);
              }
            }
            else{
              ifAttrs.put(ta.getNodeName(), ta.getNodeValue());
            }
          }
        }
      } //interface node
    } //for i
    if (!found) {
      throw new IllegalArgumentException("Sensor/actuator '"+name+"': Missing '"+INTERFACE_TAG+"' tag.");
    }
  } //method
}
