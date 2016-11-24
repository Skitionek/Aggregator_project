package dtu.is31380;

import java.awt.Color;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Orientation;
import de.erichseifert.gral.graphics.layout.TableLayout;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.ui.InteractivePanel;

public class SimpleHousePlotter extends JFrame implements Runnable {
  
  private XYPlot temp_plot;
  private XYPlot door_plot;
  private ProcessImage pi;
  private static final long TIMESTEP=5000;
  private static final int PLOTLENGTH=360;
  private Thread thread;
  private DataTable[] dt,dt2;
  private double max_temp;
  private double min_temp;
  private static final String[] tempsensors={"s_tempmain","s_tempr1","s_tempr2",
      "s_tempr3","s_tempr4","s_tempr5","s_tempr6","s_tempr7","s_tempout"};
  private static final String[] doorsensors={"s_doorx1","s_doorx2","s_doorx3",
      "s_doorx4","s_doorx5","s_doorx6","s_doorx7"};
  private static final Color[] TEMPCOLORS={
      Color.black,Color.red,Color.green,Color.blue,
      Color.gray,Color.yellow.darker().darker(),Color.cyan.darker().darker(),Color.MAGENTA,
      Color.ORANGE
  };
  
    public SimpleHousePlotter(ProcessImage pi) {
      this.pi=pi;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        max_temp=25;
        min_temp=15;
        
        dt=new DataTable[9];
        temp_plot = new XYPlot();
        temp_plot.setLegendVisible(true);
        double insetsTop = 10.0,
            insetsLeft = 10.0,
            insetsBottom = 30.0,
            insetsRight = 20.0;
        //temp_plot.getTitle().setText("Temperatures");
        temp_plot.setInsets(new Insets2D.Double(
         insetsTop, insetsLeft, insetsBottom, insetsRight));
        int tssec=Math.toIntExact(TIMESTEP/1000);
        for (int i=0;i<9;i++) {
          dt[i]=new DataTable(Integer.class,Double.class);
          DataSeries series=new DataSeries(tempsensors[i],dt[i],1);
          temp_plot.getLegend().add(series);
          dt[i].setName(tempsensors[i]);
          for (int j=0;j<PLOTLENGTH;j++) {
            dt[i].add(-tssec*(PLOTLENGTH-j),Double.NaN);
          }
          temp_plot.add(dt[i]);
          temp_plot.getLegend().remove(dt[i]);
          LineRenderer lines = new DefaultLineRenderer2D();
          lines.setColor(TEMPCOLORS[i]);
          temp_plot.setLineRenderers(dt[i], lines);
          temp_plot.setLineRenderers(series, lines);
          temp_plot.setPointRenderers(dt[i], null);
        }
        
        //temp_plot.getAxisRenderer(XYPlot.AXIS_X).getLabel().setText("Time [s]");
        temp_plot.getAxisRenderer(XYPlot.AXIS_X).setTickLabelsVisible(true);
        temp_plot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
        temp_plot.getAxis(XYPlot.AXIS_Y).setRange(min_temp, max_temp);
        temp_plot.getLegend().setOrientation(Orientation.HORIZONTAL);
        temp_plot.getLegend().setInsets(new Insets2D.Double(0,0,0,0));
        temp_plot.getLegend().setLayout(new TableLayout(2));
        
        dt2=new DataTable[7];
        door_plot=new XYPlot();
        door_plot.setLegendVisible(true);
        //door_plot.getTitle().setText("Doors");
        door_plot.setInsets(new Insets2D.Double(
         insetsTop, insetsLeft, insetsBottom, insetsRight));
        
        door_plot.getAxisRenderer(XYPlot.AXIS_X).getLabel().setText("Time [s]");
        door_plot.getAxisRenderer(XYPlot.AXIS_X).setTickLabelsVisible(true);
        door_plot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
        door_plot.getAxis(XYPlot.AXIS_Y).setRange(0,7);
        door_plot.getLegend().setOrientation(Orientation.HORIZONTAL);
        door_plot.getLegend().setInsets(new Insets2D.Double(0,0,0,0));
        door_plot.getLegend().setLayout(new TableLayout(2));
        
        for (int i=0;i<7;i++) {
          dt2[i]=new DataTable(Integer.class,Double.class);
          DataSeries series=new DataSeries(doorsensors[i],dt2[i],1);
          door_plot.getLegend().add(series);
          dt2[i].setName(doorsensors[i]);
          for (int j=0;j<PLOTLENGTH;j++) {
            dt2[i].add(-tssec*(PLOTLENGTH-j),Double.NaN);
          }
          door_plot.add(dt2[i]);
          door_plot.getLegend().remove(dt2[i]);
          LineRenderer lines = new DefaultLineRenderer2D();
          lines.setColor(TEMPCOLORS[i]);
          door_plot.setLineRenderers(dt2[i], lines);
          door_plot.setLineRenderers(series, lines);
          door_plot.setPointRenderers(dt2[i], null);
        }
        
        InteractivePanel ip=new InteractivePanel(temp_plot);
        InteractivePanel ip2=new InteractivePanel(door_plot);
        JPanel jp=new JPanel();
          jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.add(ip);
        jp.add(ip2);
        getContentPane().add(jp);
        thread=new Thread(this);
        thread.start();
        setVisible(true);
    }
    
    public void run() {
      long starttime=System.currentTimeMillis();
      while (1==1) {
        long difftime=System.currentTimeMillis()-starttime;
        int difft=Math.toIntExact(difftime/1000L);
        for (int i=0;i<9;i++) {
          String ts=tempsensors[i];
          double t=pi.getSensorValue(ts);
          if (t>max_temp) {
            max_temp+=5;
            temp_plot.getAxis(XYPlot.AXIS_Y).setRange(min_temp, max_temp);
          }
          if (t<min_temp) {
            min_temp-=5;
            temp_plot.getAxis(XYPlot.AXIS_Y).setRange(min_temp, max_temp);
          }
          dt[i].add(difft,t);
          dt[i].remove(0);
        }
        for (int i=0;i<7;i++) {
          String ts=doorsensors[i];
          double t=i+((pi.getSensorValue(ts)>0.5)?0.95:0.05);
          dt2[i].add(difft,t);
          dt2[i].remove(0);
        }
        this.repaint();
        try {
          Thread.sleep(TIMESTEP);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

}