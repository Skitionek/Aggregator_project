package commexercise;

import java.util.Arrays;

import commexercise.broadcast.BroadcastNode;
import commexercise.broadcast.BroadcastNodeImpl;
import commexercise.rpc.CallListener;
import commexercise.rpc.RpcServer;
import commexercise.rpc.RpcServerImpl;

public class Grid {

	private static final String NODE_NAME = "Grid";
	private static final int PORT = 8080;
	private static final String FUN_NAME = "D"; //keep it short
	private static boolean answear_flag = false;

  public static void main(String[] args) throws Exception {
	    
	    // create an rpc server listening on port
	    RpcServer server = new RpcServerImpl(PORT).start();
	    
	    //tell everyone what's needed
	    BroadcastNode bcn=new BroadcastNodeImpl(NODE_NAME,PORT);
	    
	    
	    bcn.sendMessage("V:45;"); //reduce length to fit into package

	    // add a call listener that will get called when a client does an RPC call to the server
	    server.setCallListener(new CallListener() {
	      @Override
	      public String[] receivedSyncCall(String function, String[] fargs) throws Exception {
	        System.out.println("Received call for function '"+function+"' with arguments"+
	                            Arrays.toString(fargs)+". Replying now.");
	        if (function.equals(FUN_NAME)) {
	          String rargs=new StringBuilder(Arrays.toString(fargs)).reverse().toString();
	          answear_flag=true;
	          return new String[]{"ACC","Sure do it!"};
	        }
	        else {
	          return new String[]{"ERR","Communication error. Wrong function."};
	        }
	      }
	
	      @Override
	      public String[] receivedAsyncCall(String function, String[] args, long callID) throws Exception {
	        return null; // not implemented for this test
	      }
	    });
	    while (!answear_flag) {
	    	System.out.println("Grid send demand");
	    	bcn.sendMessage("V:45;"); //reduce length to fit into package
	    	Thread.sleep(3000);
	    }
  }

}
