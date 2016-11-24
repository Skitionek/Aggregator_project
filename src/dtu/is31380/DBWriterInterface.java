package dtu.is31380;

import java.text.DecimalFormat;

public interface DBWriterInterface {

  String[] getHeaderFields();
  DecimalFormat[] getColumnFormats();
  Double[] getData();
  String getUnit();
}
