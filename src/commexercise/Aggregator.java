package commexercise;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commexercise.broadcast.BroadcastNode;
import commexercise.broadcast.BroadcastNodeImpl;
import commexercise.broadcast.MessageListener;
import commexercise.pubsub.PubSubServer;
import commexercise.pubsub.PubSubServerImpl;
import commexercise.pubsub.PubSubSubscriberListener;
import commexercise.pubsub.demo.ClockService;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;

public class Aggregator implements MessageListener {

	private static final String AGG_NAME = "AGG1";
	private static final String NODE_GRID = "Grid";
	private static final String NODE_AGG = "Aggregator";
	private static final int PORT = 8081;
	private static final int PORT_AGG = 9090;
	private static final String FUN_NAME = "D"; //keep it short
	
    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);

  private Aggregator() throws Exception {
	  //set lisening for broadcasting
    BroadcastNode bcn_listen=new BroadcastNodeImpl(NODE_GRID,0);
    bcn_listen.addMessageListener(this);

    //Hi! I am aggregator
    BroadcastNode bcn_publish=new BroadcastNodeImpl(NODE_AGG,PORT_AGG);
    bcn_publish.sendMessage("AGG:"+AGG_NAME+";"); //reduce length to fit into package
    
    // create a publisher server listening on port 9090
    log.info("Starting PubSub Server");
    PubSubServer pubSubServer = new PubSubServerImpl(PORT_AGG).start();

    // add subscriber listener (gets called when a client subscribes or unsubscribes)
    pubSubServer.addSubscriberListener(new PubSubSubscriberListener() {
        public void subscriberJoined(String topic, String id) {
          System.out.println("House '"+id+"' joined the aggregated control system'");
        }

        public void subscriberLeft(String topic, String id) {
          System.out.println("House '"+id+"' left the aggregated control system");
        }
    });

    // start the demo service that publishes current time to subscribers
        ClockService clock = new ClockService(pubSubServer).start();
  }

  private static int increase=0;
  private static String IP = "";
  private static int port=0;  
  
  public static void startSyncCommunication() throws Exception {
     
      RpcClient client = new RpcClientImpl(IP,port);
      // do synchronous call to server
      String args[] = {"Yes I can!"}; // to be changed
      String[] reply = client.callSync(FUN_NAME,args);
      System.out.println("Synchronous reply: "+Arrays.toString(reply));
      if (reply[0]=="ACC") {
    	  System.out.println("Send message to homes.");
	      
    	  //TODO
	      //to start next time
      

      }
      
    }
  
  @Override
  public void messageReceived(String message, String origin) {
    System.out.println("*** Received message from '"+origin+"':");
    System.out.println("    \""+message+"\"");
    String arr[] = message.split(";");
    if (arr.length == 3) {
    	for(String param:arr) {
    		String tmp[] = param.split(":");
    		switch(tmp[0]){
			case "V": increase=Integer.valueOf(tmp[1]); break;
			case "I": IP=tmp[1]; break;
			case "P": port=Integer.valueOf(tmp[1]); break;
			default: increase=0;
    		}
    	}
    	if (increase!=0 && IP!="" && port!=0) {
	        System.out.println("Decoded as:\n increase "+increase+"\n IP "+IP+"\n port "+port);
	        
	//        Can I do?
	        System.out.println("Calc if I can do.");
	        //TODO
	        
	        
	        
	        System.out.println("Start sync communication.");
				try {
					startSyncCommunication();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	} else {}; //message for somebody else
    } else System.out.println("Incomplete message!");
//    String IP[] =
  }
  
  public static void main(String args[]) throws Exception {
	  new Aggregator();
  }
  
}
