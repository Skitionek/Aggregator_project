package dtu.is31380.Communication.commexercise.pubsub;

public interface PubSubSubscriberListener {
    void subscriberJoined(String topic, String uniqueId);
    void subscriberLeft(String topic, String uniqueId);
}
