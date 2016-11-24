package dtu.is31380.aggregator;

public class TSOTest implements TSOListener {

  private long initTime;
  
  public static void main(String args[]) {
    new TSOTest();
  }

  public TSOTest() {
    TSO tso=new TSO(this);
    initTime=System.currentTimeMillis();
    while (1==1) {
      try {
        Thread.currentThread().join();
      }
      catch (InterruptedException e) {}
    }
  }
  
  @Override
  public double serviceRequest(long startTime, long endTime, double maxDelay, double minDuration,
      RegulationType regType) {
    System.out.println("["+((System.currentTimeMillis()-initTime)/1000)+
                       "s]: TSO request for service from "+startTime+"-"+endTime+", max delay="+
                       maxDelay+", min duration="+minDuration+", type="+regType.toString());
    double rv=10*(Math.random()-0.5);
    if (((rv<0) && (regType==RegulationType.DOWN)) ||
        ((rv>0) && (regType==RegulationType.UP))) {
      rv=0;
    }
    System.out.println("Offering "+rv+" kW.");
    return rv;
  }

  @Override
  public void serviceActivation(RegulationType regType, double magnitude) {
    System.out.println("["+((System.currentTimeMillis()-initTime)/1000)+
                       "s]: TSO activated service, "+magnitude+" kW requested, direction "
                       +regType.toString());
    
  }
  
}
