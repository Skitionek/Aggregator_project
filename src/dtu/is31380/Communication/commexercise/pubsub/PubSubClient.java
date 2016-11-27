package dtu.is31380.Communication.commexercise.pubsub;

public interface PubSubClient {
    void subscribe(String topic, PubSubCallbackListener observer);
    void unsubscribe(PubSubCallbackListener observer);
    void stop();
    void setID(String id);
}
