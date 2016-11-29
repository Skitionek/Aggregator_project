package Aggregator;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.EmptyStackException;

import dtu.is31380.Communication.commexercise.rpc.CallListener;
import dtu.is31380.Communication.commexercise.rpc.RpcClient;
import dtu.is31380.Communication.commexercise.rpc.RpcClientImpl;
import dtu.is31380.Communication.commexercise.rpc.RpcServer;
import dtu.is31380.Communication.commexercise.rpc.RpcServerImpl;
import dtu.is31380.TSO.TSO;
import dtu.is31380.TSO.TSOListener;

public class Grid implements TSOListener {

    private long initTime = 0;

    private RpcClient client = null;
    private static String AGG_ADDRESS = "localhost";
    public static int AGG_PORT = Aggregator.AGG_GRID_PORT;
    public static final String FUN_TIME_SYNC = "Time"; //keep it short
    public static final String FUN_REQUEST = "Request"; //keep it short
    public static final String FUN_ACTIVATION = "Activation"; //keep it short

    private RpcServer grid_server = null;
    public static final int GRID_PORT = 8083;

    private void wait_random(long ms) {
        try {
            Thread.sleep((long)(ms + ms * Math.random()));
        } catch (InterruptedException e1) {};
    }
    private void exc(String s, long ms) {
        System.out.println(s);
        wait_random(ms);
    }
    private void exc(String s) {
        exc(s + " Trying again in random time of 1-2s...", 1000);
    }

    Grid() {
        initTime = System.currentTimeMillis();

        for (boolean i = true; i;) try {
            client = new RpcClientImpl("http://" + AGG_ADDRESS + ":" + AGG_PORT);
            i = false;
        } catch (MalformedURLException e) {
            exc(i + ". Connection to aggregator refused! Check address and port.");
        }

        // set aggregator calls listener
        for (int i = 1; i <= 10; i++) try {
            grid_server = new RpcServerImpl(GRID_PORT).start();
            i = 10;
        } catch (Exception e) {
            exc(i + ". Setting grid calls listener failed!");
        }
        grid_server.setCallListener(new CallListener() {@
            Override
            public String[] receivedSyncCall(String
                function, String[] args) throws Exception {
                System.out.println("Received call for function '" + function +"' with arguments" +
                    Arrays.toString(args) + ". Replying now.");
                //Not currently implement
                return new String[] {
                    "EXC"
                };
            }@
            Override
            public String[] receivedAsyncCall(String
                function, String[] args, long callID) throws Exception {
                return null; // not implemented for this test
            }
        });


        // do synchronous call to aggregator TIME SYNCH
        String[] reply = null;
        for (int i = 1; i <= 10; i++) try {
            reply = client.callSync(FUN_TIME_SYNC, new String[] {
                String.valueOf(initTime)
            });
            if (!reply[0].equals("ACC")) throw new EmptyStackException();
            i = 10;
        } catch (Exception e) {
            exc(i + ". Time sync call exception! Possibly Aggregator is down or acknowlege message was dropped!");
        }
        System.out.println("InitTime sucessfully send to aggregator");

        // if the time is set lets start our service
        new TSO(this);
        while (1 == 1) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {}
        }
    }

    @
    Override
    public double serviceRequest(long startTime, long endTime, double maxDelay, double minDuration, RegulationType regType) {
        System.out.println("[" + ((System.currentTimeMillis() - initTime) / 1000) +
            "s]: TSO request for service from " + startTime + "-" + endTime + ", max delay=" +
            maxDelay + ", min duration=" + minDuration + ", type=" + regType.toString());

        String[] reply = null;
        // do synchronous call to aggregator REQUEST
        for (int i = 1; i <= 10; i++) try {
            reply = client.callSync(FUN_REQUEST, new String[] {
                String.valueOf(startTime),
                    String.valueOf(endTime),
                    String.valueOf(maxDelay),
                    String.valueOf(minDuration),
                    String.valueOf(regType)
            });
            i = 10;
        } catch (Exception e) {
            exc("Request call exception!");
        }
        System.out.println("Agg offering " + reply[0] + " kW.");
        return Double.valueOf(reply[0]);
    }

    @
    Override
    public void serviceActivation(RegulationType regType, double magnitude) {
        System.out.println("[" + ((System.currentTimeMillis() - initTime) / 1000) +
            "s]: TSO activated service, " + magnitude + " kW requested, direction " + regType.toString());
        String[] reply = null;
        // do synchronous call to aggregator ACTIVATION
        for (int i = 1; i <= 10; i++) try {
            reply = client.callSync(FUN_ACTIVATION, new String[] {
                String.valueOf(regType), String.valueOf(magnitude)
            });
            if (!reply[0].equals("ACC")) throw new EmptyStackException();
            i = 10;
        } catch (Exception e) {
            exc("Activation call exception!");
        }
        System.out.println("Activation message sent.");
    };

    public static void main(String[] args) {
        switch (args.length) {
            case 2:
                AGG_ADDRESS = args[1];
                System.out.println("Assigned aggregator address: " + AGG_ADDRESS);
            case 1:
                AGG_PORT = Integer.valueOf(args[0]);
                System.out.println("Assigned aggregator port: " + AGG_PORT);
                break;
            default:
                System.out.println("Used default aggregator address:port");
        }
        new Grid();
    }

}