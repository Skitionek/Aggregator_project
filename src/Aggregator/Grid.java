package Aggregator;

import java.net.MalformedURLException;
import java.util.Arrays;

import dtu.is31380.Communication.commexercise.rpc.CallListener;
import dtu.is31380.Communication.commexercise.rpc.RpcClient;
import dtu.is31380.Communication.commexercise.rpc.RpcClientImpl;
import dtu.is31380.Communication.commexercise.rpc.RpcServer;
import dtu.is31380.Communication.commexercise.rpc.RpcServerImpl;
import dtu.is31380.TSO.TSO;
import dtu.is31380.TSO.TSOListener;

public class Grid implements TSOListener {
	
    private long initTime = 0;
	private static String AGG_ADDRESS = "localhost";
	public static int AGG_PORT = Aggregator.AGG_GRID_PORT;
	public static final String FUN_TIME_SYNC = "Time"; //keep it short
	public static final String FUN_REQUEST =  "Request"; //keep it short
	public static final String FUN_ACTIVATION =  "Activation"; //keep it short
	private RpcClient client = null;
	
	private RpcServer grid_server = null;
	public static final int GRID_PORT = 8083;
	
	Grid() {
	    initTime=System.currentTimeMillis();
		
		try {
			client = new RpcClientImpl("http://"+AGG_ADDRESS+":"+AGG_PORT);
		} catch (MalformedURLException e) {
			System.out.println("Connection refused!");
			//e.printStackTrace();
		}

		try {
			grid_server = new RpcServerImpl(GRID_PORT).start();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			//e2.printStackTrace();
		}
		
		String[] reply = null;
		// do synchronous call to agg TIME SYNCH
		while(reply==null) {
			try {
				reply = client.callSync(FUN_TIME_SYNC, new String[]{String.valueOf(initTime)});
			} catch (Exception e) {
				System.out.println("Time call exception! Possibly Agg is down!");
				System.out.println("Retrying in one second...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
				}
				//e.printStackTrace();
			}
			System.out.println(Arrays.toString(reply));	
		}
		
		grid_server.setCallListener(new CallListener() {
	  	      @Override
	  	      public String[] receivedSyncCall(String function, String[] args) throws Exception {
	  	        System.out.println("Received call for function '"+function+"' with arguments"+
	  	                            Arrays.toString(args)+". Replying now.");
	  	        
	  	        return new String[]{"EXC"};
	  	      }
	  	      @Override
	  	      public String[] receivedAsyncCall(String function, String[] args, long callID) throws Exception {
	  	        return null; // not implemented for this test
	  	      }
	  	    });
		
		// if the time is set lets start our service
		new TSO(this);
//	    while (true) {
//	      try {
//	        Thread.currentThread().join();
//	      }
//	      catch (InterruptedException e) {}
//	    }
	}
	  
	  @Override
	  public double serviceRequest(long startTime, long endTime, double maxDelay, double minDuration,RegulationType regType) {
	    System.out.println("["+((System.currentTimeMillis()-initTime)/1000)+
			                   "s]: TSO request for service from "+startTime+"-"+endTime+", max delay="+
			                   maxDelay+", min duration="+minDuration+", type="+regType.toString());

			String[] reply = null;
			// do synchronous call to agg REQUEST
			try {
				reply = client.callSync(FUN_REQUEST, new String[]{
					String.valueOf(startTime),
					String.valueOf(endTime),
					String.valueOf(maxDelay),
					String.valueOf(minDuration),
					String.valueOf(regType)
				});
			} catch (Exception e) {
				System.out.println("Request call exception!");
				//e.printStackTrace();
			}
			System.out.println(Arrays.toString(reply));
			
			System.out.println("Agg offering "+reply[0]+" kW.");
			    return Double.valueOf(reply[0]);
	  }
	
	  @Override
	  public void serviceActivation(RegulationType regType, double magnitude) {
	    System.out.println("["+((System.currentTimeMillis()-initTime)/1000)+
	                   "s]: TSO activated service, "+magnitude+" kW requested, direction "
	                       +regType.toString());
		    String[] reply = null;
			// do synchronous call to agg REQUEST
			try {
				reply = client.callSync(FUN_ACTIVATION, new String[]{String.valueOf(regType),String.valueOf(magnitude)});
			} catch (Exception e) {
				System.out.println("Activation call exception!");
				//e.printStackTrace();
			}
			System.out.println(Arrays.toString(reply));
	  };
    
	public static void main(String[] args) {
		switch(args.length) {
			case 2:
				AGG_ADDRESS=args[1];
					System.out.println("Assigned port");
			case 1:
				AGG_PORT=Integer.valueOf(args[0]);
					System.out.println("Assigned address");break;
			default:
				System.out.println("Used default address:port");
		}
		new Grid();
	}

}
