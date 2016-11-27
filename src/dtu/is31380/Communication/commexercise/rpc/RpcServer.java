package dtu.is31380.Communication.commexercise.rpc;

public interface RpcServer {
    RpcServer start() throws Exception;
    void stop();
    void setCallListener(CallListener listener);
    void removeCallListener(CallListener listener);
}
