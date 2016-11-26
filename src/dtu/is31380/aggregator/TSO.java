package dtu.is31380.aggregator;

import dtu.is31380.aggregator.TSOListener.RegulationType;

public class TSO implements Runnable {
  
  private static final double MAX_DELAY = 5; //[s]
  private static final double MIN_DURATION = 20; //[s]
  public static final long CONTRACTING_TIME=60*1000; //after 1min
  public static final long CONTRACT_INTERVAL=2*60*1000; //every 2min
  public static final long RESOLUTION=1000; //run everything on a 1s basis
  
  private TSOListener listener;
  private long clockZero;
  private long currentContract;
  private long nextContract;
  private RegulationType currentRegType;
  private RegulationType nextRegType;
  private double currentRegMagn;
  private double nextRegMagn;
  private Thread thread;
  
  public TSO(TSOListener listener) {
    this.listener=listener;
    clockZero=System.currentTimeMillis();
    currentContract=clockZero;
    nextContract=clockZero+CONTRACT_INTERVAL;
    currentRegType=null;
    nextRegType=null;
    currentRegMagn=0;
    nextRegMagn=0;
    thread=new Thread(this);
    thread.start();
  }

  @Override
  public void run() {
    long t1=clockZero+RESOLUTION;
    boolean activated=false;
    while (!Thread.interrupted()) {
      if (t1>=(currentContract+CONTRACTING_TIME)) {
        if (nextRegType==null) {
          nextRegType=(Math.random()>0.5?RegulationType.UP:RegulationType.DOWN);
          nextRegMagn=listener.serviceRequest((currentContract-clockZero)/1000,
                                              (nextContract-clockZero)/1000,
                                              MAX_DELAY, MIN_DURATION, nextRegType);
        }
      }
      if (t1>=nextContract) {
        nextContract+=CONTRACT_INTERVAL;
        currentContract+=CONTRACT_INTERVAL;
        currentRegMagn=nextRegMagn;
        currentRegType=nextRegType;
        nextRegMagn=0;RegulationType
        nextRegType=null;
        activated=false;
      }
      if (Math.random()>0.5/(CONTRACT_INTERVAL/RESOLUTION)) {
        if ((!activated) && (currentRegType!=null) && (currentRegMagn>0)) {
          activated=true;
          listener.serviceActivation(currentRegType, (0.5+Math.random()*0.5)*currentRegMagn);
        }
      }
      try {
        Thread.sleep(Math.max(0, t1-System.currentTimeMillis()));
      }
      catch (InterruptedException e) {}
      t1+=RESOLUTION;
    }
  }
}
