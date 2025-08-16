package com.ads.apiseng.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.Message;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PubSubService {
    
    private final Map<String, Object> pubSubConfig = new ConcurrentHashMap<>();
    private final Map<String, List<TopicMessage>> topicMessages = new ConcurrentHashMap<>();
    
    public void publishMessage(String topic, Message<?> message) {
        // Implementation for publishing messages
        System.out.println("Publishing message to topic: " + topic);
        System.out.println("Message payload: " + message.getPayload());
        
        // Store message for retrieval
        topicMessages.computeIfAbsent(topic, k -> new ArrayList<>())
                    .add(new TopicMessage(message.getPayload().toString(), System.currentTimeMillis()));
    }
    
    public void subscribeToTopic(String topic) {
        // Implementation for subscribing to topics
        System.out.println("Subscribing to topic: " + topic);
    }
    
    public Map<String, Object> getConfiguration() {
        return new ConcurrentHashMap<>(pubSubConfig);
    }
    
    public void updateConfiguration(Map<String, Object> config) {
        pubSubConfig.putAll(config);
    }
    
    public boolean isConnected() {
        // Implementation to check connection status
        return true;
    }
    
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("connected", isConnected());
        status.put("configuration", getConfiguration());
        return status;
    }
    
    public List<TopicMessage> getTopicMessages(String topic) {
        return topicMessages.getOrDefault(topic, new ArrayList<>());
    }
    
    public void publishToSubscribers(String topic, String content, List<String> subscribers) {
        System.out.println("Publishing to subscribers for topic: " + topic);
        if (subscribers != null) {
            subscribers.forEach(subscriber -> {
                System.out.println("Notifying subscriber: " + subscriber + " with content: " + content);
            });
        }
    }
    
    // Inner class for topic messages
    public static class TopicMessage {
        private final String content;
        private final long timestamp;
        
        public TopicMessage(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
}
