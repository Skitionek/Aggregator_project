package dtu.is31380;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleHouseSimulator {

  private HWModule hwModule;
  private ProcessImage procImg;
  private LoggerManager loggerMgr;
  private SimpleHousePlotter plot;
  
  public SimpleHouseSimulator(String cfgfile, String controller) throws IOException {
    long simtimeref=System.currentTimeMillis();
    
    System.out.println("Initializing LoggerManager");
    loggerMgr = new LoggerManager();

    System.out.println("Starting HW simulator");
    hwModule=new HWModule(cfgfile, loggerMgr, simtimeref);

    // Get the ProcessImage
    procImg = hwModule.getProcessImage();

    plot=new SimpleHousePlotter(procImg);
    
    if (controller!=null) {
      try {
        Class<AbstractHouseController> c=(Class<AbstractHouseController>) Class.forName(controller);
        AbstractHouseController hc=c.newInstance();
        hc.setInterface(procImg);
      }
      catch (ClassNotFoundException e) {
        System.out.println("Controller class '"+controller+"' could not be found. Exiting.");
        System.exit(1);
      }
      catch (InstantiationException e) {
        System.out.println("Controller class '"+controller+"' could not be instantiated. (Subclass of AbstractHouseController?) Exiting.");
        System.exit(1);
      }
      catch (IllegalAccessException e) {
        System.out.println("Controller class '"+controller+"' does not have a public constructor. Exiting.");
        System.exit(1);
      }
    }
    
  }

  private void shutdown() {
    procImg.shutdown();
    hwModule.stopHWThreads();
    /*loggerMgr.getPlatformEventLogger().logEvent("Core", 
        "NOTICE", "-------------- PLATFORM STOPPED -------------- ");*/
    loggerMgr.stopLoggers();
  }

  /**
   * Logs to the standard console and to a text file
   */
  public static class ConsoleLogStream extends PrintStream {
    private final PrintStream mFileStream;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
    private Date mDate = new Date();

    public ConsoleLogStream(OutputStream consoleStream, PrintStream fileStream) {
      super(consoleStream);
      this.mFileStream = fileStream;
    }

    @Override
    public void close() {
      super.close();
    }

    @Override
    public void flush() {
      super.flush();
      mFileStream.flush();
    }

    @Override
    public void print(String s) {
      // TODO Auto-generated method stub
      mDate.setTime(System.currentTimeMillis());
      super.print(s);
      mFileStream.print(mFormat.format(mDate)+"   "+s);
      mFileStream.println();
    }
  }

  private String escape(String msg) {
    msg = msg.replace("\\", "\\\\");
    msg = msg.replace("\"", "\\\"");
    msg = msg.replace("\n", "\\n");
    return msg;
  }

  public static void main(String args[]) {
      // Pipe stdout and stderr to files aswell as to the standard console
      /*String identifier = new SimpleDateFormat("dd-MM-yyyy--HH-mm").format(new Date());
      try {
        System.setErr(new ConsoleLogStream(System.err, new PrintStream(new File("tmp/Error_"+ identifier +".txt"))));
        System.setOut(new ConsoleLogStream(System.out, new PrintStream(new File("tmp/Out_"+ identifier +".txt"))));
      }
      catch (IOException e) {
        System.out.println("Could not open console log output files. Shutting down.");
        System.exit(1);
      }*/

    try {
      if (args.length==0) {
        System.out.println("No config file specified. Shutting down.");
        System.exit(1);
      }
      String configFile = args[0];
      String controller=null;
      if (args.length>1) {
        controller=args[1];
      }
      final SimpleHouseSimulator core=new SimpleHouseSimulator(configFile,controller);

      // Add a shutdownhook to properly shut down the threads in the processimage
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          System.out.println("Shutting Down house simulator platform!");
          if (core!=null) {
            core.shutdown();
          }
        }
      });
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
