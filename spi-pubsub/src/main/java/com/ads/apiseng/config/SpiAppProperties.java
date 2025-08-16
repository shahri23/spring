package com.ads.apiseng.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spi-app")
public class SpiAppProperties {
    
    private Channels channels = new Channels();
    private Routing routing = new Routing();
    private PubSub pubsub = new PubSub();
    private Processing processing = new Processing();
    private ErrorHandling errorHandling = new ErrorHandling();
    
    // Getters and Setters
    public Channels getChannels() { return channels; }
    public void setChannels(Channels channels) { this.channels = channels; }
    
    public Routing getRouting() { return routing; }
    public void setRouting(Routing routing) { this.routing = routing; }
    
    public PubSub getPubsub() { return pubsub; }
    public void setPubsub(PubSub pubsub) { this.pubsub = pubsub; }
    
    public Processing getProcessing() { return processing; }
    public void setProcessing(Processing processing) { this.processing = processing; }
    
    public ErrorHandling getErrorHandling() { return errorHandling; }
    public void setErrorHandling(ErrorHandling errorHandling) { this.errorHandling = errorHandling; }
    
    // Nested Configuration Classes
    public static class Channels {
        private boolean enabled = true;
        private ContentRouter contentRouter = new ContentRouter();
        private OriginalTransformer originalTransformer = new OriginalTransformer();
        private PubSubConfig pubSub = new PubSubConfig();
        
        // Basic properties
        private String input = "inputChannel";
        private String output = "outputChannel";
        private String error = "errorChannel";
        private int capacity = 100;
        private int threadPoolSize = 10;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public ContentRouter getContentRouter() { return contentRouter; }
        public void setContentRouter(ContentRouter contentRouter) { this.contentRouter = contentRouter; }
        
        public OriginalTransformer getOriginalTransformer() { return originalTransformer; }
        public void setOriginalTransformer(OriginalTransformer originalTransformer) { this.originalTransformer = originalTransformer; }
        
        public PubSubConfig getPubSub() { return pubSub; }
        public void setPubSub(PubSubConfig pubSub) { this.pubSub = pubSub; }
        
        // Basic property getters/setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public static class ContentRouter {
            private boolean enabled = true;
            private boolean parallelProcessing = true;
            private boolean errorHandling = true;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public boolean isParallelProcessing() { return parallelProcessing; }
            public void setParallelProcessing(boolean parallelProcessing) { this.parallelProcessing = parallelProcessing; }
            
            public boolean isErrorHandling() { return errorHandling; }
            public void setErrorHandling(boolean errorHandling) { this.errorHandling = errorHandling; }
        }
        
        public static class OriginalTransformer {
            private boolean enabled = true;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
        
        public static class PubSubConfig {
            private boolean enabled = true;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
    }
    
    public static class Routing {
        private boolean enabled = true;
        private String defaultRoute = "default";
        private Map<String, String> routes;
        private Map<String, XmlTypeConfig> xmlTypes;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getDefaultRoute() { return defaultRoute; }
        public void setDefaultRoute(String defaultRoute) { this.defaultRoute = defaultRoute; }
        
        public Map<String, String> getRoutes() { return routes; }
        public void setRoutes(Map<String, String> routes) { this.routes = routes; }
        
        public Map<String, XmlTypeConfig> getXmlTypes() { return xmlTypes; }
        public void setXmlTypes(Map<String, XmlTypeConfig> xmlTypes) { this.xmlTypes = xmlTypes; }
        
        public static class XmlTypeConfig {
            private boolean enabled = true;
            private String channel;
            private String transformer;
            private List<String> subscribers;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getChannel() { return channel; }
            public void setChannel(String channel) { this.channel = channel; }
            
            public String getTransformer() { return transformer; }
            public void setTransformer(String transformer) { this.transformer = transformer; }
            
            public List<String> getSubscribers() { return subscribers; }
            public void setSubscribers(List<String> subscribers) { this.subscribers = subscribers; }
        }
    }
    
    public static class PubSub {
        private boolean enabled = false;
        private String provider = "default";
        private Map<String, Object> configuration;
        private MessageBroker messageBroker = new MessageBroker();
        private Map<String, String> topics;
        private Map<String, SubscriberConfig> subscribers;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        
        public MessageBroker getMessageBroker() { return messageBroker; }
        public void setMessageBroker(MessageBroker messageBroker) { this.messageBroker = messageBroker; }
        
        public Map<String, String> getTopics() { return topics; }
        public void setTopics(Map<String, String> topics) { this.topics = topics; }
        
        public Map<String, SubscriberConfig> getSubscribers() { return subscribers; }
        public void setSubscribers(Map<String, SubscriberConfig> subscribers) { this.subscribers = subscribers; }
        
        public static class MessageBroker {
            private String type = "in-memory";
            private boolean enabled = true;
            
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
        
        public static class SubscriberConfig {
            private boolean enabled = true;
            private List<String> topics;
            private boolean async = true;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public List<String> getTopics() { return topics; }
            public void setTopics(List<String> topics) { this.topics = topics; }
            
            public boolean isAsync() { return async; }
            public void setAsync(boolean async) { this.async = async; }
        }
    }
    
    public static class Processing {
        private boolean parallelProcessing = true;
        private int maxRetries = 3;
        private long retryDelay = 1000;
        private int threadPoolSize = 10;
        private long timeoutMs = 5000;
        private int retryAttempts = 3;
        private boolean enableMetrics = true;
        private boolean enableTracing = true;
        
        public boolean isParallelProcessing() { return parallelProcessing; }
        public void setParallelProcessing(boolean parallelProcessing) { this.parallelProcessing = parallelProcessing; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public long getRetryDelay() { return retryDelay; }
        public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
        
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        
        public boolean isEnableTracing() { return enableTracing; }
        public void setEnableTracing(boolean enableTracing) { this.enableTracing = enableTracing; }
    }
    
    public static class ErrorHandling {
        private boolean enabled = true;
        private String strategy = "log";
        private List<String> notificationChannels;
        private int maxRetryAttempts = 3;
        private boolean deadLetterQueue = true;
        private String errorChannel = "errorChannel";
        private String retryPolicy = "exponential-backoff";
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        
        public List<String> getNotificationChannels() { return notificationChannels; }
        public void setNotificationChannels(List<String> notificationChannels) { this.notificationChannels = notificationChannels; }
        
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
        
        public boolean isDeadLetterQueue() { return deadLetterQueue; }
        public void setDeadLetterQueue(boolean deadLetterQueue) { this.deadLetterQueue = deadLetterQueue; }
        
        public String getErrorChannel() { return errorChannel; }
        public void setErrorChannel(String errorChannel) { this.errorChannel = errorChannel; }
        
        public String getRetryPolicy() { return retryPolicy; }
        public void setRetryPolicy(String retryPolicy) { this.retryPolicy = retryPolicy; }
    }
}
