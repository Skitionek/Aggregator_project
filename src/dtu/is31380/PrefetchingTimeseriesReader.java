package dtu.is31380;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class PrefetchingTimeseriesReader implements Runnable {

  public static final String TIMESTRING="TIME";
  private static final String PTR="PrefetchingTimeseriesReader";
  
  private String[] channels;
  private String[] headers;
  private TimeseriesListener listener;
  private File tsfile;
  private boolean running;
  private long reftime;
  private BufferedReader read;
  private String thisline;
  private String nextline;
  private long thisTimestamp;
  private long nextTimestamp;
  private int colmap[]; //Contains column indexes mapping input file columns to columns in headers[] 
                        //entry j in colmap points to the column in the input file that the data for column j is coming from
  private Thread thr;
  private Hashtable<String,Double> outputData;
  
  public PrefetchingTimeseriesReader(String filename, long timeOffset, TimeseriesListener listener,
                                     String[] channels, String[] headers) {
    
    this.listener=listener;
    this.channels=channels;
    read=null;
    this.headers=headers;
    tsfile=new File(filename);
    if ((!tsfile.exists()) || (!tsfile.isFile())) {
      throw new IllegalArgumentException("Timeseries file does not exist: "+filename);
    }
    outputData=new Hashtable<String,Double>();
    nullifyData(outputData);
  }
  
  public void start() {
    reftime=System.currentTimeMillis();
    thisline=null;
    nextline=null;
    colmap=null;
    try {
      read=new BufferedReader(new FileReader(tsfile));
      thisline=read.readLine();
      if (thisline==null) {
        throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
            "' must contain at least a header and two data lines.");
      }
      else {
        parseHeader(thisline);
        thisline=read.readLine();
        nextline=read.readLine();
        if ((thisline==null) || (nextline==null)) {
          throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
              "' must contain at least a header and two data lines.");
        }
        else {
          Vector<Number> l1=parseLine(thisline);
          Vector<Number> l2=parseLine(nextline);
          thisTimestamp=reftime+(Long)l1.elementAt(0);
          nextTimestamp=reftime+(Long)l2.elementAt(0);
        }
      }
    }
    catch (IOException e) {
      listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "I/O Exception: "+e.getMessage());
    }
    catch (IllegalStateException e) {
      listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "Illegal state exception: "+e.getMessage());
    }
    listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "Initialization complete");

    thr=new Thread(this);
    thr.start(); 
  }
  
  public void stop() {
    running=false;
    thr.interrupt();
    //nullifyData();
    if (read!=null) {
      try {
        read.close();
      }
      catch (IOException e) {}
      read=null;
    }
  }
  
  public void reset() {
    
  }
  
  private void nullifyData(Hashtable<String,Double> h) {
    for (String s:channels) {
      h.put(s, Double.NaN);
    }
  }
  
  private Vector<Number> parseLine(String line) {
    Vector<Number> rv=new Vector<Number>();
    if (line!=null) {
      String[] atoms=line.split(",");
      if (atoms.length<2) {
        throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
            "': needs at least one timestamp column and one data column.");
      }
      String convert="";
      try {
        Long ts=Long.valueOf(atoms[0]);
        rv.add(ts);
        for (int i=0;i<headers.length;i++) {
          int srccolindex=colmap[i];
          if (srccolindex>=atoms.length) {
            listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "Did not file source column for '"+headers[i]+
                                   "' (column "+srccolindex+").");
            srccolindex=-1;
            colmap[i]=-1;
          }
          if (srccolindex!=-1) {
            convert=atoms[i+1];
            rv.add(Double.valueOf(convert));
          }
          else {
            rv.add(Double.NaN);
          }
        }
      }
      catch (NumberFormatException e) {
        throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
            "': '"+convert+"' is not a valid number.");
      }
    } 
    return rv;
  }
  
  private void parseHeader(String line) {
    String[] hdritems=line.split(",");
    if (!hdritems[0].toLowerCase().startsWith(TIMESTRING.toLowerCase())) {
      throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
                                      "': Leftmost column name must start with '"+TIMESTRING+"'.");
    }
    colmap=new int[headers.length];
    Arrays.fill(colmap, -1);
    boolean match;
    for (int i=1;i<hdritems.length;i++) { 
      match=false;
      for (int j=0;j<headers.length;j++) {
        if (hdritems[i].toLowerCase().startsWith(headers[j].toLowerCase())) {
          colmap[j]=i; 
          match=true;
        } 
      }
      if (!match) {
        listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "Unknown column name: '"+hdritems[i]+"'. Ignoring.");
      }
    }
    for (int i=0;i<colmap.length;i++) {
      if (colmap[i]==-1) {
        throw new IllegalStateException("Timeseries file '"+tsfile.getAbsolutePath()+
            "': does not contain column for channel '"+channels[i]+"'.");
      }
    }    
  }
  
  @Override
  public void run() {
    running=true;
    while (running) {
      try {
        long deltat=Math.max(0,(thisTimestamp-System.currentTimeMillis()));
        Thread.sleep(deltat);
        
        thisline=nextline;
        nextline=read.readLine();
        Vector<Number> l1=parseLine(thisline);
        thisTimestamp=reftime+(Long)l1.elementAt(0);
        Double[] rv=new Double[l1.size()-1];
        for (int j=0;j<rv.length;j++) {
          rv[j]=new Double(l1.elementAt(colmap[j]).doubleValue());
        }
        listener.update(rv);
      }
      catch (InterruptedException e) {
        running=false;
      }
      catch (IOException e) {
        listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "I/O Exception: "+e.getMessage());
      }
      catch (IllegalStateException e) {
        listener.logEvent(PTR, "Timeseries file '"+tsfile+"'", "Illegal state: "+e.getMessage());
      }      
    }
    //nullifyData();
  }

}
