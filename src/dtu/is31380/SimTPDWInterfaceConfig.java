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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings("serial")
public class SimTPDWInterfaceConfig extends HWInterfaceConfig implements Serializable {

  public static final String TYPESTRING="SimThermalPowerDoorsWindows";
  private static final String DSOURCE_ATTR="datasource";
  private static final String TSFILE_ATTR="tsfile";
  private static final String LOOP_ATTR="loop";

  private static final String SRCTYPE_FILE = "file";
  
  public static final int SRCENUM_UNKNOWN = 0;
  public static final int SRCENUM_FILE = 1;
  
  private int source;
  private String tsfilename;
  private boolean loop;
  
  public SimTPDWInterfaceConfig() {
    super();    
    loop=false;
    tsfilename=null;
    source=SRCENUM_UNKNOWN;
  }
  
  protected void parseSubInterface(NamedNodeMap attrs) {
    Node _dsource=attrs.getNamedItem(DSOURCE_ATTR);
    if (_dsource!=null) {
      switch(_dsource.getNodeValue()) {
        case SRCTYPE_FILE:
          source=SRCENUM_FILE;
          break;
        default:
          throw new IllegalArgumentException(HWINTERFACE_TAG+": '"+DSOURCE_ATTR+"' attribute supports only 'file'.");
      }
    }
    else {
      source=SRCENUM_FILE; //hardcode this for the moment since it's the only option implemented in the simulator  
      //throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+DSOURCE_ATTR+"' attribute.");
    }
    
    if (source==SRCENUM_FILE) {
      Node _tsfile=attrs.getNamedItem(TSFILE_ATTR);
      if (_tsfile!=null) {
        tsfilename=_tsfile.getNodeValue();
        System.out.println("tsfilename="+tsfilename);
      }
      else {
        throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+TSFILE_ATTR+"' attribute.");
      }

      Node _loop=attrs.getNamedItem(LOOP_ATTR);
      if (_loop!=null) {
        switch (_loop.getNodeValue().toLowerCase()) {
          case "yes":
          case "y":
          case "1":
          case "true":
            loop=true;
            break;
          case "no":
          case "n":
          case "0":
          case "false":
            loop=false;
            break;
          default:
            throw new IllegalArgumentException(HWINTERFACE_TAG+": '"+LOOP_ATTR+"' attribute must be yes/y/1/true or no/n/0/false.");
        }
      }
      else {
        loop=false;
        //throw new IllegalArgumentException(HWINTERFACE_TAG+": Missing '"+LOOP_ATTR+"' attribute.");
      }
    }
    
  }

  public String getType() {
    return TYPESTRING;
  }
  
  protected void toStringImpl(StringBuilder b) {
  }

  public HardwareInterface getInterfaceInstance(BuildingConfig buc) {
    return new TPDWSimulationInterface(this,buc);
  }

  @Override
  public String getAddress() {
    return null;
  }
  
  public String getTimeseriesFile() {
    return tsfilename;
  }
  
}
