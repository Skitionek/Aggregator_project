package dtu.is31380;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/*
 * Copyright (c) 2012-2014, Technical University of Denmark (DTU)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import dtu.is31380.SystemConstants.DeviceType;

/*
 * Simulates (T)emperature and heater (P)ower consumption under consideration of (D)oor and (W)indow opening state
 */

public class TPDWSimulationInterface extends HardwareInterface
                                     implements Runnable,
                                                TimeseriesListener {
  
  private static final DecimalFormatSymbols symb=new DecimalFormatSymbols();
  
  static {
    symb.setNaN("NaN");
    symb.setInfinity("Inf");
    }

  private static final DecimalFormat THREEDECIMALS=new DecimalFormat("##0.000",symb);
  
  public static final String ATTR_CHANNEL="channel";
  
  public static final double TRANSMIT_OUTSIDE=1.3; //[W/m2K] 
  public static final double TRANSMIT_INSIDE=5.0; //[W/m2K] drywall, doors open
  public static final double TRANSMIT_ROOF=0.5; //[W/m2K] wild guess
  public static final double TRANSMIT_FLOOR=1.0; //[W/m2K] wild guess
  public static final double HEATCAP_AIR=0.001297; //[J/cm3K]
  public static final double HEATCAP_WALL=0.8; //[J/cm3K]
//  public static final double ROOMHEIGHT=2.5; //[m]
  public static final double RATEDU = 400.0; //[V]
  public static final double RATEDF = 50.0; //[Hz]
//  private static final double UPPERSETPT = 27.0;
//  private static final double LOWERSETPT = 25.5;
  //how much of the energy irradiated onto the windows actually stays in the building.
  //For realistic performance (close to Flexhouse) use values between 0.2 and 0.3.
  //We're setting this to 0.1 by default, in order to get a bit more action out of the
  //simulated houses in the summer months.
  private static final double WINFACTOR = 0.1;  
  private static final double INITTEMP=21;
  
  // Mappings
  private HashMap<String, String> tempSensorRoomTable; //key=sensor name, value=name of room the sensor is in
  private HashMap<String, String> doorSensorRoomTable; //key=sensor name, value=name of room the sensor is in
  private HashMap<String, String> windowSensorRoomTable; //key=sensor name, value=name of room the sensor is in
  private HashMap<String, String> heaterRoomTable; //key=actuator name, value=name of room the actuator/heater is in
  private HashMap<String, String> windowActuatorRoomTable; //key=actuator name, value=name of room the actuator/heater is in
  private HashMap<String,RoomConfig> rooms; //key=room name, value=calculated room data
  private HashMap<String,Double> heaterPowerTable; //key=actuator name, value=rated power in kW;
  private HashMap<String,Double> roomTemperatures;
  private HashMap<String,Boolean> heaterStates;
  private HashMap<String,Boolean> windowStates;
  private HashMap<String,Boolean> doorStates;
  private HashMap<String,String> channelMap; //key: channel name; value: sensor name
  
  private boolean connected;
  
  private Double airtemp; //[degC]
  private Double windspeed; //[msec-1]
  private Double winddir; //[deg from north]
  private Double irrad; //[kW/m2]
  private double lastValidAirtemp;
  private double lastValidIrrad;
  private boolean running;
  private double kwhcounter; //[kWh]
  private long timestep; //in milliseconds
  private Thread simthr;
  private PrefetchingTimeseriesReader ptr;
  
  private static final String PSUMSENSOR="s_Grid_Psum";
  private static final String KWHSENSOR="s_Grid_Pimp";
  String[] HEADERS={"TEMPERATURE_temp1[degC]","WINDSPEED_wspd1[m/s]","WINDDIR_wdir1[deg]",
                    "INSOLATION_irrad1[kW/m2]","INSOLATION_irrad2[kW/m2]","door1","door2",
                    "doorx1","doorx2","doorx3","doorx4","doorx5","doorx6","doorx7"};
  String[] CHANNELS={"TEMP","WSPD","WDIR","SOL1","SOL2","door1","door2",
                     "doorx1","doorx2","doorx3","doorx4","doorx5","doorx6","doorx7"};
  String[] HCCHANNELS={"temp","insol","door1","door2","doorx1","doorx2","doorx3","doorx4","doorx5","doorx6","doorx7"};

  public TPDWSimulationInterface(HWInterfaceConfig hic, BuildingConfig buc) {
    super(hic,buc);
    timestep=5000;
    airtemp=new Double(5);
    windspeed=new Double(5);
    winddir=new Double(270);
    irrad=new Double(0.150);
    lastValidAirtemp=airtemp.doubleValue();
    lastValidIrrad=irrad.doubleValue();
    kwhcounter=0;
    connected=false;
    if (hic instanceof SimTPDWInterfaceConfig) {
      SimTPDWInterfaceConfig sic=(SimTPDWInterfaceConfig)hic;
      //System.out.println("tsfile="+sic.getTimeseriesFile());
      try {
        ptr=new PrefetchingTimeseriesReader(sic.getTimeseriesFile(),0,this,CHANNELS,HEADERS);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void startInterfaceImpl(ArrayList<SensorConfig> sensors, ArrayList<ActuatorConfig> actuators) {
    //System.out.println("Starting TPDW interface impl");
    tempSensorRoomTable=new HashMap<String, String>();
    doorSensorRoomTable=new HashMap<String, String>();
    windowSensorRoomTable=new HashMap<String, String>();
    heaterRoomTable=new HashMap<String, String>();
    windowActuatorRoomTable=new HashMap<String, String>();
    heaterPowerTable=new HashMap<String,Double>();
    rooms=new HashMap<String,RoomConfig>();
    
    // Map
    for (SensorConfig sensor : sensors){
      try {
        if (sensor.getType() == DeviceType.Temperature) {
          RoomConfig room=sensor.getRoom();
          if (room!=null) {
            if (!rooms.containsKey(room.getName())) {
              rooms.put(room.getName(),room);
            }
            String rox=tempSensorRoomTable.get(sensor.getName());
            if ((rox!=null) && (rox.equals(room.getName()))) {
              throw new IllegalArgumentException("Sensor already assigned: "+sensor.getName());
            }
            else {
              tempSensorRoomTable.put(sensor.getName(),room.getName());
            }
          }
        }
        
        else if (sensor.getType() == DeviceType.Electrical) {
          //do nothing
        }
        
        else if (sensor.getType() == DeviceType.Window) {
          RoomConfig room=sensor.getRoom();
          if (room!=null) {
            if (!rooms.containsKey(room.getName())) {
              rooms.put(room.getName(),room);
            }
            String rox=windowSensorRoomTable.get(sensor.getName());
            if ((rox!=null) && (rox.equals(room.getName()))) {
              throw new IllegalArgumentException("Sensor already assigned: "+sensor.getName());
            }
            else {
              windowSensorRoomTable.put(sensor.getName(),room.getName());
            }
          }
        }
        
        else if (sensor.getType() == DeviceType.Door) {
          RoomConfig room=sensor.getRoom();
          if (room!=null) {
            if (!rooms.containsKey(room.getName())) {
              rooms.put(room.getName(),room);
            }
            String rox=doorSensorRoomTable.get(sensor.getName());
            if ((rox!=null) && (rox.equals(room.getName()))) {
              throw new IllegalArgumentException("Sensor already assigned: "+sensor.getName());
            }
            else {
              doorSensorRoomTable.put(sensor.getName(),room.getName());
            }
          }
        }
        
        else if (sensor.getType() == DeviceType.Insolation) {
        //do nothing??
        }
        
        else{
          System.out.println(getName()+" Error: Cannot handle sensor type "+sensor.getType());
          /*getLogger().logEvent(getName(), "Error", "Cannot handle sensor type: "+sensor.getType()+", for sensor: "
              +sensor.getName());*/
        }

      } catch (Exception e) {
        System.out.println(getName()+" Error mapping "+sensor.getName()+": "+e.getMessage());
        //getLogger().logEvent(getName(), "Error", "Error mapping sensor: " + sensor.getName()+": "+e.getMessage());
      }
    }
    for (ActuatorConfig actuator : actuators){
      try{
        if (actuator.getType()==DeviceType.PowerControlledHeater) {
          RoomConfig room=actuator.getRoom();
          if (room!=null) {
            if (!rooms.containsKey(room.getName())) {
              rooms.put(room.getName(),room);
            }
            String rox=heaterRoomTable.get(actuator.getName());
            if ((rox!=null) && (rox.equals(room.getName()))) {
              throw new IllegalArgumentException("Actuator already assigned: "+actuator.getName());
            }
            else {
              heaterRoomTable.put(actuator.getName(),room.getName());
              heaterPowerTable.put(actuator.getName(), actuator.getRatedActivePowerConsumption());
            }
          }
        }
        else if (actuator.getType()==DeviceType.Window) {
          RoomConfig room=actuator.getRoom();
          if (room!=null) {
            if (!rooms.containsKey(room.getName())) {
              rooms.put(room.getName(),room);
            }
            String rox=windowActuatorRoomTable.get(actuator.getName());
            if ((rox!=null) && (rox.equals(room.getName()))) {
              throw new IllegalArgumentException("Actuator already assigned: "+actuator.getName());
            }
            else {
              windowActuatorRoomTable.put(actuator.getName(),room.getName());
            }
          }
        }
        else{
          System.out.println(getName()+" Error: Cannot handle actuator type "+actuator.getType());
          /*getLogger().logEvent(getName(), "Error", "Cannot handle actuator tyope: "+actuator.getType()+", for actuator: "
              +actuator.getName());*/
        }
      } catch (NumberFormatException e) {
        System.out.println(getName()+" Error mapping "+actuator.getName()+": "+e.getMessage());
        //getLogger().logEvent(getName(), "Error", "Error mapping actuator: " + actuator.getName()+": "+e.getMessage());
      }
    }
    mapTSChannelsToSensors(sensors,HCCHANNELS);
    roomTemperatures=new HashMap<String,Double>();
    heaterStates=new HashMap<String,Boolean>();
    windowStates=new HashMap<String,Boolean>();
    doorStates=new HashMap<String,Boolean>();
    Iterator<String> rit=rooms.keySet().iterator();
    while (rit.hasNext()) {
      roomTemperatures.put(rit.next(), INITTEMP);
    }
    
    Iterator<String> hit=heaterRoomTable.keySet().iterator();
    while (hit.hasNext()) {
      heaterStates.put(hit.next(),Boolean.FALSE);
    }
    
    ifData.clear();
    Iterator<String> wit=windowSensorRoomTable.keySet().iterator();
    while (wit.hasNext()) {
      String witnext=wit.next();
      windowStates.put(witnext,Boolean.FALSE); //initially closed
      ifData.put(witnext, 0.0);
    }
    notifyProcessImage();
    
    Iterator<String> dit=doorSensorRoomTable.keySet().iterator();
    while (dit.hasNext()) {
      doorStates.put(dit.next(),Boolean.FALSE); //initially closed
    }
    //getLogger().logEvent(getName(), "Initialization", "Complete");
    simthr=new Thread(this);
    simthr.start();
    ptr.start();
  }

  @Override
  protected void stopInterfaceImpl() {
    running=false;
    ptr.stop();
    simthr.interrupt();
  }

  public boolean probeConnection() {
    // No way to check connection
    return false;
  }

  private void mapTSChannelsToSensors(ArrayList<SensorConfig> sensors, String[] channels) {
    //Map channels to sensors
    channelMap=new HashMap<String,String>();
    for (String chan:channels) {
      for (SensorConfig sc:sensors) {
        HashMap<String,String> attrs=sc.getInterfaceAttributes();
        String ch=attrs.get(ATTR_CHANNEL);
        if (ch!=null) {
          if (ch.equals(chan)) {
            channelMap.put(chan, sc.getName());
          }
        }
        else {
          System.out.println(sc.getName()+"': Interface does not have a '"
              +ATTR_CHANNEL+"' attribute. Sensor cannot be mapped.");
          //getLogger().logEvent(getName(), "Error", sc.getName()+"': Interface does not have a '"
              //+ATTR_CHANNEL+"' attribute. Sensor cannot be mapped.");
        }
      }
    }
  }
  
  public void run() {
    connected=true;
    long starttime=System.currentTimeMillis();
    long nexttime=starttime+timestep;
    running=true;
    while (running) {
      //System.out.println("TIME="+((System.currentTimeMillis()-starttime)/1000));
      double totalKW=getTotalkW();
      kwhcounter+=(totalKW*((((double)timestep)/1000)/3600));
      double avgTemp=0;
      Iterator<String> rit=roomTemperatures.keySet().iterator();
      while (rit.hasNext()) {
        avgTemp+=roomTemperatures.get(rit.next());
      }
      avgTemp/=(double)roomTemperatures.size();
//      System.out.println("house: avgTemp="+avgTemp);
      synchronized(ifData) {
        ifData.clear();
        ifData.put(PSUMSENSOR, new Double(totalKW));
        ifData.put(KWHSENSOR, new Double(kwhcounter));
        //System.out.print("temp=[");
        rit=roomTemperatures.keySet().iterator();
        while (rit.hasNext()) {
          ArrayList<Double> tempadj=new ArrayList<Double>();
          ArrayList<Double> areaadj=new ArrayList<Double>();
          String rist=rit.next(); //name of the room we're looking at
          RoomConfig roc=rooms.get(rist);
          HashMap<AbstractSpaceConfig,ArrayList<AbstractSpaceConnectorConfig>> adj=roc.getAdjacentSpaces();
          for (AbstractSpaceConfig asc:adj.keySet()) { //look at all neighbouring rooms
            ArrayList<AbstractSpaceConnectorConfig> doorwins=adj.get(asc); //which doors or windows connect us to the neighbouring room?
            for (AbstractSpaceConnectorConfig doorwin:doorwins) {
              ArrayList<SensorConfig> sensors=doorwin.getSensors(); //which sensor looks at the state of that door or window?
              for (SensorConfig sc:sensors) {
//                System.out.println("OOOOOOO Room "+rist+" connected to room "+asc.getName()+" via opening "+doorwin.getName()+
//                                   ",sensor "+sc.getName());
                Boolean ds=doorStates.get(sc.getName());
                Boolean ws=windowStates.get(sc.getName());
                if (((ds!=null) && (ds==true)) || ((ws!=null) && (ws==true))) {
                  tempadj.add(roomTemperatures.get(asc.getName()));
                  areaadj.add(doorwin.getArea());
                }
              }
            }
            
          }
          double newrt=getTemperatureUpdate(rist,avgTemp,timestep,tempadj,areaadj);
          roomTemperatures.put(rist, newrt);
          for (String srt:tempSensorRoomTable.keySet()) {
            if (tempSensorRoomTable.get(srt).equals(rist)) {
              ifData.put(srt,new Double(newrt));
            }
          }
          //System.out.print(THREEDECIMALS.format(newrt)+" / ");
        }
        //System.out.println();
        if (connected) {
          notifyProcessImage();
        }
      } //sync block
      long deltatime=Math.max(0,nexttime-System.currentTimeMillis());
      try {
          Thread.sleep(deltatime);
      }
      catch (InterruptedException e) {
          e.printStackTrace();
      }
      nexttime+=timestep;
    }
    connected=false;
  }
  
  private double getTemperatureUpdate(String roomName, double avgTemp, double dt,
      ArrayList<Double> tempInAdjoiningWithOpenDoors, ArrayList<Double> openDoorAreas) {
    if (!airtemp.isNaN())
      lastValidAirtemp=airtemp.doubleValue();
    if (!irrad.isNaN())
      lastValidIrrad=irrad.doubleValue();
    RoomConfig rd=rooms.get(roomName);
    double rt=roomTemperatures.get(roomName);
    //calculate heat exchange between rooms, and between rooms and environment 
    double heatExchangeTransmission=rd.getOuterWallArea()*(rt-lastValidAirtemp)*TRANSMIT_OUTSIDE+
                                    rd.getInnerWallArea()*(rt-avgTemp)*TRANSMIT_INSIDE+
                                    rd.getFloorArea()*(rt-lastValidAirtemp)*TRANSMIT_ROOF+
                                    rd.getFloorArea()*(rt-lastValidAirtemp)*TRANSMIT_FLOOR; //outgoing, in Watt
    
    //Convection exchange formula taken from http://members.questline.com/Article.aspx?articleID=21224
    double heatExchangeConvection=0;
    for (int i=0;i<openDoorAreas.size();i++) {
      double tia=tempInAdjoiningWithOpenDoors.get(i);
      //System.out.println("i="+i+", tia="+tia+", rt="+rt+", oda="+openDoorAreas.get(i));
      double heatdiff=9.81*(rt-tia)/tia;
      double heatsqrt=(heatdiff>0)?Math.sqrt(heatdiff):-Math.sqrt(-heatdiff);
      heatExchangeConvection-=(1/3)*(openDoorAreas.get(i)*0.6)*heatsqrt;
    }
    double irradiation=rd.getOuterWindowArea()*lastValidIrrad*1000*WINFACTOR; //incoming, in kw/m2
    //calculate heat exchange from (active) heaters in room
    double heaterPower=0;
    for (String hrt:heaterRoomTable.keySet()) {
      if (heaterRoomTable.get(hrt).equals(roomName)) { //is the heater we're looking at located in this room?
        Boolean onOff=heaterStates.get(hrt);
        if (onOff!=null) {
          heaterPower+=onOff.booleanValue()?heaterPowerTable.get(hrt)*1000:0;
        }
      }
    }
    double heatCapacity=1e6*(rd.getRoomHeight()*rd.getFloorArea()*HEATCAP_AIR+
                            (rd.getOuterWallArea()+rd.getInnerWallArea()+
                             rd.getFloorArea()*2)*HEATCAP_WALL);
    // 1e6 because volumes are in m3, heatcaps are in cm3
    /*System.out.println("hP"+heaterPower+", trm="+heatExchangeTransmission+
                       ", cnv="+heatExchangeConvection+", irr="+irradiation+
                       ", dt="+dt+", hCap="+heatCapacity);*/
    double deltaTemp=((heaterPower+irradiation-heatExchangeTransmission-heatExchangeConvection)*dt)/heatCapacity;
    /*System.out.println("room "+roomName+", tmp="+rt+", irr="+irradiation+
                       ", hex="+(heatExchangeTransmission-heatExchangeConvection)+
                       ", htr="+heaterPower+", cap="+heatCapacity+", del="+deltaTemp);*/
    return roomTemperatures.get(roomName)+deltaTemp;
  }
  
  private double getTotalkW() {
    double psum=0;
    for (String h:heaterStates.keySet()) {
      if (heaterStates.get(h).booleanValue()) {
        psum+=heaterPowerTable.get(h);
      }
    }
    return psum;
  }
  
  @Override
  public void applyActuation(Actuator actuator) {
    //System.out.println("applyAct:"+actuator.getName()+"->"+actuator.getSetpoint());
    if (actuator.getType() == DeviceType.PowerControlledHeater){
      // Get the setpoint
      boolean on = (((actuator.getSetpoint().doubleValue()) > 0.5) ? true: false);
      if (heaterStates.containsKey(actuator.getName())) {
        synchronized(ifData) {
          ifData.clear();
          heaterStates.put(actuator.getName(), on);
          ifData.put(actuator.getName(),new Double(on?1.0:0.0));
          //notifyProcessImage(); Is this needed to be able to read back from the actuator?
        }
      }
    }
    else if (actuator.getType() == DeviceType.Window) {
      // Get the setpoint
      boolean open = (((actuator.getSetpoint().doubleValue()) > 0.5) ? true: false);
      if (windowStates.containsKey(actuator.getName())) {
        synchronized(ifData) {
          ifData.clear();
          windowStates.put(actuator.getName(), open);
          ifData.put(actuator.getName(),new Double(open?1.0:0.0));
          //notifyProcessImage(); Is this needed to be able to read back from the actuator?
        }
      }
    }
    else{
      System.out.println("Attempted actuation of non-actuator type: " + 
          actuator.getName());
      /*getLogger().logEvent(getName(), "Error", "Attempted actuation of non-actuator type: " + 
                  actuator.getName());*/
    }
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @Override
  public void update(Double[] data) {
    
    //TODO: hardcoded to save time right now. Need to change!
    ifData.clear();
    ifData.put(channelMap.get(HCCHANNELS[0]), data[0]); //temperature
    airtemp=data[0];
    ifData.put(channelMap.get(HCCHANNELS[1]), data[3]); //irradiation
    irrad=data[3];
    //System.out.print("doors=[");
    for (int i=0;i<9;i++) {
      ifData.put(channelMap.get(HCCHANNELS[2+i]), data[5+i]);
      doorStates.put(channelMap.get(HCCHANNELS[2+i]),(data[5+i].doubleValue()>0.5));
      //System.out.print(((data[5+i].doubleValue()>0.5)?"open":"closed")+" / ");
    }
    //System.out.println();
    notifyProcessImage();
  }

  @Override
  public void logEvent(String source, String type, String message) {
    //getLogger().logEvent(source, type, message);
  }
}

