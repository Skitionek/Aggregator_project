package commexercise;

import java.util.Arrays;

import commexercise.broadcast.BroadcastNode;
import commexercise.broadcast.BroadcastNodeImpl;
import commexercise.broadcast.MessageListener;
import commexercise.pubsub.*;
import commexercise.pubsub.demo.ClockService;
import commexercise.rpc.RpcClient;
import commexercise.rpc.RpcClientImpl;

public class HouseController implements MessageListener {
	
	public static String NAME = "house1";	//TODO to be inicjalised in constructor
	private static final String NODE_AGG = "Aggregator";
	private static final int PORT = 8082;
	private static final int PORT_PUB = 9090;
    
	private String agg_name="";
	private static String IP="";
    private static int port=0;
    
	public HouseController() throws Exception { //TODO to be change to init()

  	  //set broadcasting
      BroadcastNode bcn_listen=new BroadcastNodeImpl(NODE_AGG,0);
      bcn_listen.addMessageListener(this);
      
        
    }
	
	public static void suscribeToAggregator() throws Exception {
		// create a pubsub client
        PubSubClient pubSubClient = new PubSubClientImpl(IP,PORT_PUB,NAME);

        // subscribe to clock topic
        PubSubCallbackListener clockListener = new PubSubCallbackListener() {
            public void messageReceived(String[] msg) {
                System.out.println("Received: " + msg[0]);
            }
        };
        System.out.println("Subscribing to aggregator service.");
        pubSubClient.subscribe("clock", clockListener);


//        System.out.println("Unsubscribing from aggregator service.");
//        pubSubClient.unsubscribe(clockListener);
//        pubSubClient.stop();	      
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
  			case "AGG": agg_name=tmp[1]; break;
  			case "I": IP=tmp[1]; break;
  			case "P": port=Integer.valueOf(tmp[1]); break;
  			default: agg_name="";
      		}
      	}
      	if (agg_name!="" && IP!="" && port!=0) {
          System.out.println("Decoded as:\n aggregator name "+agg_name+"\n IP "+IP+"\n port "+port);
          
          System.out.println("Open suscribtion channel.");
  			try {
  				suscribeToAggregator();
  			} catch (Exception e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
      	} else {} //mesagge to somebody else
      } else System.out.println("Incomplete message!");
//      String IP[] =
    }
    

    public static void main(String args[]) throws Exception {  // to run it from commandline
  	  new HouseController();
    }
}
