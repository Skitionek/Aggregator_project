package dtu.is31380;

public interface TimeseriesListener {

  public void update(Double[] data);
  public void logEvent(String source, String type, String message);
  
}
