package dtu.is31380;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class NumericalLogger extends AbstractDBWriter implements DBWriterInterface {

  private String[] colHeaders;
  private DecimalFormat[] colFormats;
  private ProcessImage procimg;
  private Double[] rv;
  private String unit;

  public NumericalLogger(ProcessImage pi, String unit) {
    super();
    procimg=pi;
    
    ArrayList<String> columns=new ArrayList<String>();
    ArrayList<DecimalFormat> formats=new ArrayList<DecimalFormat>();
    Sensor[] sens=pi.getSensors();
    for (Sensor s:sens) {
      columns.add("S_"+s.getName()+"["+s.getUnit()+"]");
      formats.add(THREEDECIMALS);
    }
    Actuator[] actu=pi.getActuators();
    for (Actuator a:actu) {
      columns.add("A_"+a.getName()+"_s["+a.getUnit()+"]");
      columns.add("A_"+a.getName()+"_v["+a.getUnit()+"]");
      formats.add(THREEDECIMALS);
      formats.add(THREEDECIMALS);
    }
    
    colHeaders=new String[columns.size()];
    colHeaders=columns.toArray(colHeaders);
    colFormats=new DecimalFormat[formats.size()];
    colFormats=formats.toArray(colFormats);
    rv=new Double[colHeaders.length];

    this.unit = unit.replace('-', '_').replace(' ', '_').toLowerCase();
  }
  
  @Override
  public String[] getHeaderFields() {
    return colHeaders;
  }
  
  @Override
  public DecimalFormat[] getColumnFormats() {
    return colFormats;
  }

  @Override
  public Double[] getData() {
    Sensor[] sens=procimg.getSensors();
    int writePtr=0;
    for (Sensor s:sens) {
      rv[writePtr++]=s.getValue();
    }
    Actuator[] actu=procimg.getActuators();
    for (Actuator a:actu) {
      rv[writePtr++]=a.getSetpoint();
      rv[writePtr++]=a.getValue();
    }
    return rv;
  }

  @Override
  public String getUnit() { return unit; }

}
