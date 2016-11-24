package dtu.is31380;

import dtu.is31380.DataLogger.DbWriters;

/**
 * Central logging facility. Spawns three separate loggers: NumericalLogger, 
 * PlatformEventLogger and HouseEventLogger.
 * @author olge
 */
public class LoggerManager {

  private static final int INTERVAL = 1000; //ms
  private static final int ROTATION = 86400; //seconds
  private static String NUMLOGPATH = "tmp/";
  private static String PLATFORMLOGPATH = "tmp/";
  private static String HOUSELOGPATH = "tmp/";
  private NumericalLogger numlog;
  //private HouseEventLogger houselog;
  //private PlatformEventLogger plaflog;
  private DataLogger logger;
  private DbWriters writers;
  private boolean running;
  private String errMessage;
  
  public LoggerManager() {
  NUMLOGPATH = ".";
  //HOUSELOGPATH = ".";
  //PLATFORMLOGPATH = ".";

    //plaflog=new PlatformEventLogger();
    //houselog=new HouseEventLogger();
    logger=new DataLogger();
    writers = new DbWriters();
    writers.fileWriter = new DBFileWriter();
    numlog = null;
    running=false;
  }
  
  public void startLoggers(ProcessImage pi, BuildingConfig config) {
    //plaflog.start(ROTATION, PLATFORMLOGPATH);
    //houselog.start(ROTATION, HOUSELOGPATH);
    numlog = new NumericalLogger(pi, config.getBuildingName());
    writers.simpledbDbWriter = numlog;
    errMessage = logger.start(writers, INTERVAL, ROTATION, NUMLOGPATH, null, 0);
    running = (errMessage == null);
  }
  
  public void stopLoggers() {
    running=false;
    logger.stop();
    //houselog.stop();
    //plaflog.stop();
  }

  public NumericalLogger getNumericalLogger() {
    return numlog;
  }
  
  /*public PlatformEventLogger getPlatformEventLogger() {
    return plaflog;
  }
  
  public HouseEventLogger getHouseEventLogger() {
    return houselog;
  }*/
  
}
