package dtu.is31380;

/*
 * Copyright (c) 2012-2015, Technical University of Denmark (DTU)
 * All rights reserved.
 * 
 * The Flexhouse 2.0 platform ("this software") is licensed under the
 * BSD 3-clause license.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of DTU nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific
 *     prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE TECHNICAL UNIVERSITY OF DENMARK BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.Serializable;
import java.util.ArrayList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class DoorConfig extends AbstractSpaceConnectorConfig implements Serializable {

  public static final String DOOR_TAG="door";
  private static final String NAME_ATTR="name";
  private static final String TO_ATTR="to";
  private static final String AREA_ATTR="area";

  private ArrayList<AbstractSpaceConfig> rooms; //spaces connected by this door
  
  public String toString() {
    StringBuffer rv=new StringBuffer("WindowConfig \""+name+"\" {\n");
    rv.append("}\n");
    return rv.toString();
  }
  
  public static DoorConfig parse(Node in, RoomConfig rc) {
    if ((in.getNodeType()==Node.ELEMENT_NODE) && (DOOR_TAG.equals(in.getNodeName()))) {
      NamedNodeMap attrs=in.getAttributes();
      String dn;
      String to;
      double area;
      
      Node _doorName=attrs.getNamedItem(NAME_ATTR);
      if (_doorName!=null) {
        dn=_doorName.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(DOOR_TAG+": Missing '"+NAME_ATTR+"' attribute.");
      }
      
      Node _doorTo=attrs.getNamedItem(TO_ATTR);
      if (_doorTo!=null) {
        to=_doorTo.getNodeValue();
      }
      else {
        throw new IllegalArgumentException(DOOR_TAG+": Missing '"+TO_ATTR+"' attribute.");
      }
      
      Node _doorArea=attrs.getNamedItem(AREA_ATTR);
      if (_doorArea!=null) {
        try {
          area=Double.valueOf(_doorArea.getNodeValue());
        }
        catch (NumberFormatException e) {
          area=Double.NaN;
          throw new IllegalArgumentException(DOOR_TAG+": Numerical value required for '"+AREA_ATTR+"' attribute.");
        }
      }
      else {
        throw new IllegalArgumentException(DOOR_TAG+": Missing '"+AREA_ATTR+"' attribute.");
      }
      
      DoorConfig dc=new DoorConfig(dn,rc,to,area);
      return dc;
    } //correct node
    return null;
  }
  
  private DoorConfig(String name,RoomConfig rc,String toName,double area) {
    super();
    this.name=name;
    this.connectedToName=toName;
    this.area=area;
    rooms=new ArrayList<AbstractSpaceConfig>();
    rooms.add(rc);
  }
  
  public double getDoorArea() {
    return area;
  }

  public ArrayList<AbstractSpaceConfig> getLinkedSpaces() {
    return rooms;
  }
  
  public boolean matchSpace(AbstractSpaceConfig match) {
    if (connectedToName.equals(match.name)) {
      rooms.add(match);
      match.addAdjacentSpace(rooms.get(0),this);
      rooms.get(0).addAdjacentSpace(match,this);
      return true;
    }
    return false;
  }
  
}

