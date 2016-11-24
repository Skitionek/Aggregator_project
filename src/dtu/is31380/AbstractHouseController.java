package dtu.is31380;

public abstract class AbstractHouseController implements Runnable {
  
  protected HouseControllerInterface house;
  private Thread thr;
  private long timestep;
  private static final long TIMESTEP=1000;
  
  protected AbstractHouseController() {
    this(0);
  }
  
  protected AbstractHouseController(long timestep) {
    if (timestep==0) {
      this.timestep=TIMESTEP;
    }
    else {
      this.timestep=timestep;
    }
  }
  
  public void setInterface(HouseControllerInterface hci) {
    house=hci;
    thr=new Thread(this);
    thr.start();
  }
  
  protected HouseControllerInterface getInterface() {
    return house;
  }
  
  protected abstract void execute();
  
  protected abstract void init();

  public void run() {
    init();
    long nextTime=System.currentTimeMillis()+timestep;
    while (1==1) {
      execute();
      long dt=nextTime-System.currentTimeMillis();
      if (dt<0) {
        dt=0;
        System.out.println("execute() method takes longer than timestep!");
      }
      try {
        Thread.sleep(dt);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      nextTime+=timestep;
    }
  }
  
}
