package dtu.is31380;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataLogger implements Runnable {
  private static final int MIN_INTERVAL=100; // Don't write more than 10 values/sec
  private static final int MAX_INTERVAL=43200000; // Don't accept intervals larger than half a day

  protected int writeInterval;
  protected Thread writerThread;
  private long t0,t1,td;
  private boolean running;
  Double[] cd;
  private ExecutorService executorService;
  DbWriters writers;

  public DataLogger() {
    writerThread=null;
    running=false;
    writers = null;
    cd = null;
  }

  public void stop() {
    running=false;
    if (writerThread!=null) {
      writerThread.interrupt();
    }
  }

  public void run() {
    Thread.currentThread().setName( getClass().getName() + " " + writers.simpledbDbWriter.getUnit() );
    t0=System.currentTimeMillis();
    t0-=(t0%writeInterval); //round down to next multiple of writeInterval as starting time. This should never become negative.
    try {
      writers.fileWriter.dbOpen(t0);
      td=t0+writeInterval;
      t1=t0;
      while (running) {
        t1=System.currentTimeMillis();
        while (td>t1) {
          try {
            Thread.sleep((td>t1)?td-t1:0);
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running=false;
            break;
          }
          t1=System.currentTimeMillis();
        }

        // write to file asynchronously
        if (writers.fileWriter != null) {
          executorService.execute(new Runnable() {
            @Override
            public void run() {
              try {
                writers.fileWriter.dbCheck(t1);
                writers.fileWriter.dbWrite(t1);
              }
              catch (IOException e) {
                e.printStackTrace();
              }
            }
          });
        }

        td+=writeInterval; 
      } //while running
      writers.fileWriter.dbClose();
      executorService.shutdown();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Deprecated
  public String start(DbWriters writers, int interval, int rotate, String dbPath) {
    return start(writers, interval, rotate, dbPath, null, 0);
  }

  public String start(DbWriters writers, int interval, int rotate, String dbPath, String connectionString, int rotateHistory) {
    this.writers = writers;

    if (interval<MIN_INTERVAL) {
      writeInterval=MIN_INTERVAL;
    }
    else if (interval>MAX_INTERVAL) {
      writeInterval=MAX_INTERVAL;
    }
    else {
      writeInterval=interval;
    }

    if (writers == null) return "DBWriters must be instanced before they can be used";

    if (writers.fileWriter == null) return "The fileWriter in DBWriters must be instanced";
    String te=writers.fileWriter.initialize(writers.simpledbDbWriter, rotate, dbPath);
    if (te != null) {
      System.out.println("Unable to initialize SimpleDB FileWriter: " + te);
      return te;
    }

    executorService = Executors.newFixedThreadPool(10);

    running=true;
    writerThread=new Thread(this);
    writerThread.start();
    return null;
  }

  public static class DbWriters {
    public DBWriterInterface simpledbDbWriter  = null;
    public DBFileWriter fileWriter             = null;
  }
}
