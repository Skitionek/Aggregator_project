package dtu.is31380;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Common base class of all SimpleDB writer classes. Contains common definitions.
 * @author olge, orda
 */

public abstract class AbstractDBWriter {
  
  private static final DecimalFormatSymbols symb=new DecimalFormatSymbols();

  static {
    symb.setNaN("NaN");
    symb.setInfinity("Inf");
  }
  
  protected static final DecimalFormat ZERODECIMALS=new DecimalFormat("###0",symb);
  protected static final DecimalFormat ONEDECIMAL=new DecimalFormat("###0.0",symb);
  protected static final DecimalFormat TWODECIMALS=new DecimalFormat("##0.00",symb);
  protected static final DecimalFormat THREEDECIMALS=new DecimalFormat("##0.000",symb);
  protected static final DecimalFormat FOURDECIMALS=new DecimalFormat("##0.0000",symb);
  
  /* -----------Guidelines for rounding-----------------
   * Power [kW, kVA, kVAr]: 3 digits (resolution 1W)
   * Voltage [V]: 1 digit (resolution .1V)
   * Energy [kWh, kVAh, kVArh]: 3 digits (resolution 1Wh)
   * Frequency [Hz]: 2 digits (resolution 10mHz)
   * Speed [m/s]: 1 digit (resolution .1m/s)
   * Temperature [degC]: 1 digit (resolution .1C)
   * Cos phi: 3 digits
   */

}
