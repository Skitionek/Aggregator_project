package dtu.is31380.Communication.commexercise.pubsub;

public interface PubSubCallbackListener {
    void messageReceived(String[] message);
}
