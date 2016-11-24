package dtu.is31380;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Common base class of all SimpleDB writer classes. Contains code for timekeeping, db creation, file rotation etc.
 * @author olge
 */

public class DBFileWriter {

  public static final String VERSION="2.0";
  private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
  private static final String FILESUFFIX=".csv";
  private static final String FIRSTLINE="SimpleDB data file version "+VERSION;

  private DBWriterInterface dbWriter;
  private File dbPath;
  protected int rotateInterval; //is not used at the moment; we force daily rotation.
  private Calendar currentStartOfDay;
  private Calendar nextStartOfDay;
  private File writeFile;
  private PrintStream output;
  private DecimalFormat[] colFormats;

  public DBFileWriter() {
    writeFile=null;
    dbPath=null;
    output=null;
    dbWriter = null;
  }

  private void makeOutputHandle() throws IOException {
    int fileSuffix=0;
    boolean opened=false;
    String columnHeaderString=getColumnHeaderString();
    while (!opened) {
      writeFile=new File(makeFileName(dbPath,currentStartOfDay,fileSuffix));
      if ((writeFile.exists()) && (writeFile.length()>0)) {
        if (isHeaderIdentical(writeFile,columnHeaderString)) {
          output=new PrintStream(new BufferedOutputStream(new FileOutputStream(writeFile,true)));
          opened=true;
        }
        else {
          fileSuffix++;
        }
      }
      else {
        output=new PrintStream(new BufferedOutputStream(new FileOutputStream(writeFile,false)));
        writeHeader(output, columnHeaderString);
        opened=true;
      }
    } //while !opened
  }
  
  public void dbOpen(long time) throws IOException {
    Calendar c=new GregorianCalendar(UTC);
    c.setTimeInMillis(time);
    currentStartOfDay=new GregorianCalendar(UTC);
    currentStartOfDay.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),0,0,0);
    nextStartOfDay=new GregorianCalendar(UTC);
    nextStartOfDay.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),0,0,0);
    nextStartOfDay.add(Calendar.DAY_OF_MONTH,1);
    makeOutputHandle();
  }
  
  public void dbCheck(long time) throws IOException {
    if (time>=nextStartOfDay.getTimeInMillis()) {
      if (output!=null) {
        output.close();
        output=null;
      }
      writeFile=null;
      currentStartOfDay=nextStartOfDay;
      nextStartOfDay=new GregorianCalendar(UTC);
      nextStartOfDay.set(currentStartOfDay.get(Calendar.YEAR),currentStartOfDay.get(Calendar.MONTH),
                         currentStartOfDay.get(Calendar.DAY_OF_MONTH),0,0,0);
      nextStartOfDay.add(Calendar.DAY_OF_MONTH,1);
      makeOutputHandle();
    }
  }
  
  public void dbWrite(long time) {
    if (output!=null) {
      writeLine(time,output);
    }
  }

  private String getColumnHeaderString() {
    StringBuilder hdr=new StringBuilder("TIMESTAMP");
    String[] colhdrs=dbWriter.getHeaderFields();
    for (String s:colhdrs) {
      hdr.append(",");
      hdr.append(s);
    }
    return hdr.toString();
  }
  
  private void writeHeader(PrintStream output, String columnHeaderString) {
    output.println(FIRSTLINE);
    output.println(columnHeaderString);
  }
  
  private boolean isHeaderIdentical(File f, String columnHeaderString) {
    try {
      BufferedReader inr = new BufferedReader(new FileReader(f));
      String l1=inr.readLine();
      if ((l1==null) || (!l1.equals(FIRSTLINE))) {
        inr.close();
        return false;
      }
      String l2=inr.readLine();
      if ((l2==null) || (!l2.equals(columnHeaderString))) {
        inr.close();
        return false;
      }
      inr.close();
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }
  
  private void writeLine(long timestamp, PrintStream output) {
    StringBuilder lbuf=new StringBuilder(Long.valueOf(timestamp).toString());
    Double[] cd=dbWriter.getData();
    if (cd.length!=colFormats.length) {
      output.println("Error! Length of data does not equal length of field definitions!");
    }
    else {
      for (int i=0;i<cd.length;i++) {
        lbuf.append(",");
        if (colFormats[i]==null) {
          lbuf.append(cd[i].doubleValue());
        }
        else {
          lbuf.append(colFormats[i].format(cd[i].doubleValue()));
        }
      }
      output.println(lbuf.toString());
      
      //!!!!!!!!!!!!!!!!!!!!!!!!!!
      // Please do *not* insert a flush command here. This class is also used on nodes with Flash memory disks
      // where flushing after each line may dramatically shorten the lifetime of the disk, with data loss
      // as a potential result. 
      // If you think you absolutely need flushing, please take the time to add it as a default-off configuration
      // option for the logger, and use it with care. But you should really ask yourself why you need this
      // in the first place. You may be trying to use this database class for something it was never designed
      // for. -olge, 2/1/2014
      //!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
  }
  
  public void dbClose() throws IOException {
    if (output!=null) {
      output.close();
      output=null;
    }
    writeFile=null;
    dbPath=null;
  }
  
  private String makeFileName(File path,Calendar date, int suffix) throws IOException {
    String pn=path.getCanonicalPath();
    StringBuilder fn=new StringBuilder(pn);
    if (!pn.endsWith("/")) {
      fn.append('/');
    }
    String unit = dbWriter.getUnit().replace('-', '_').replace(' ', '_').toLowerCase();
    fn.append(unit);
    fn.append('_');
    int year=date.get(Calendar.YEAR);
    fn.append(year);
    int month=1+date.get(Calendar.MONTH);
    fn.append((month<10)?"0":"");
    fn.append(month);
    int day=date.get(Calendar.DAY_OF_MONTH);
    fn.append((day<10)?"0":"");
    fn.append(day);
    fn.append(FILESUFFIX);
    if (suffix>0) {
      fn.append(".");
      fn.append(suffix);
    }
    return fn.toString();
  }

  public String initialize(DBWriterInterface writer, int r, String path) {
    if (writer == null) {
      return "DBWriter cannot be null";
    }
    dbWriter = writer;

    if (r<0) {
      rotateInterval=-1;
    }
    else {
      rotateInterval=r;
    }

    path = path.toLowerCase();
    String unit = dbWriter.getUnit().replace('-', '_').replace(' ', '_').toLowerCase();
    if (!path.endsWith("/")) path = path + "/";
    dbPath=new File(path+unit);
    if (!dbPath.isDirectory()) {
      dbPath.mkdirs();
    }
    if (!dbPath.isDirectory()) {
      return "'dbPath' field must contain the path to a directory.";
    }
    if (!dbPath.canWrite()) {
      return "Write permissions not available for directory '"+path+"'.";
    }

    colFormats=dbWriter.getColumnFormats();

    return null;
  }
  
}
