package com.ads.apiseng.config;

import com.ads.apiseng.XmlToJsonTransformer;
import com.ads.apiseng.config.SpiAppProperties;
import com.ads.apiseng.service.PubSubService;
import com.ads.apiseng.service.XmlTransformationService;
import com.ads.apiseng.util.XmlTypeDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableIntegration
@EnableConfigurationProperties(SpiAppProperties.class)
public class EnhancedIntegrationConfig {

    @Autowired
    private SpiAppProperties spiAppProperties;

    @Autowired(required = false)
    private XmlTransformationService transformationService;

    @Autowired(required = false)
    private PubSubService pubSubService;

    private final Map<String, MessageChannel> dynamicChannels = new ConcurrentHashMap<>();

    // =================== SHARED COMPONENTS ===================
    
    @Autowired
    private XmlMapper xmlMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessagingTemplate messagingTemplate;

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(spiAppProperties.getProcessing().getThreadPoolSize());
        executor.setMaxPoolSize(spiAppProperties.getProcessing().getThreadPoolSize() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("spi-async-");
        executor.initialize();
        return executor;
    }

    // =================== ORIGINAL XML→JSON TRANSFORMER (Conditional) ===================
    
    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.original-transformer.enabled", havingValue = "true")
    public MessageChannel xmlInputChannel() {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(createLoggingInterceptor("Original XML Input"));
        return channel;
    }

    @ServiceActivator(inputChannel = "xmlInputChannel")
    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.original-transformer.enabled", havingValue = "true")
    public XmlToJsonTransformer xmlToJsonTransformer() {
        System.out.println("🔧 Creating Original XML→JSON Transformer (Configurable)");
        return new XmlToJsonTransformer(xmlMapper, objectMapper);
    }

    // =================== CONTENT-BASED ROUTER CHANNELS (Conditional) ===================
    
    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.content-router.enabled", havingValue = "true")
    public MessageChannel contentRouterInputChannel() {
        MessageChannel channel;
        if (spiAppProperties.getChannels().getContentRouter().isParallelProcessing()) {
            ExecutorChannel executorChannel = new ExecutorChannel(taskExecutor());
            executorChannel.addInterceptor(createLoggingInterceptor("Content Router Input"));
            channel = executorChannel;
        } else {
            DirectChannel directChannel = new DirectChannel();
            directChannel.addInterceptor(createLoggingInterceptor("Content Router Input"));
            channel = directChannel;
        }
        return channel;
    }

    // Dynamic channel creation based on configuration
    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.content-router.enabled", havingValue = "true")
    public Map<String, MessageChannel> processingChannels() {
        Map<String, MessageChannel> channels = new ConcurrentHashMap<>();
        
        spiAppProperties.getRouting().getXmlTypes().forEach((type, config) -> {
            if (config.isEnabled()) {
                MessageChannel channel = new DirectChannel();
                channels.put(config.getChannel(), channel);
                dynamicChannels.put(config.getChannel(), channel);
                System.out.println("📡 Created processing channel: " + config.getChannel() + " for type: " + type.toUpperCase());
            }
        });
        
        return channels;
    }

    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.content-router.enabled", havingValue = "true") 
    public Map<String, MessageChannel> outputChannels() {
        Map<String, MessageChannel> channels = new ConcurrentHashMap<>();
        
        spiAppProperties.getRouting().getXmlTypes().forEach((type, config) -> {
            if (config.isEnabled()) {
                String outputChannelName = config.getChannel().replace("Processing", "Output");
                MessageChannel channel = new DirectChannel();
                channels.put(outputChannelName, channel);
                dynamicChannels.put(outputChannelName, channel);
                System.out.println("📤 Created output channel: " + outputChannelName + " for type: " + type.toUpperCase());
            }
        });
        
        return channels;
    }

    // =================== CONFIGURABLE CONTENT-BASED ROUTER ===================
    
    @Router(inputChannel = "contentRouterInputChannel")
    @ConditionalOnProperty(name = "spiapp.channels.content-router.enabled", havingValue = "true")
    public String routeXmlByContent(String xmlPayload) {
        String xmlType = XmlTypeDetector.detectType(xmlPayload).toLowerCase();
        System.out.println("🔀 CONFIGURABLE ROUTER: Detected Type = " + xmlType.toUpperCase());
        
        var xmlTypeConfig = spiAppProperties.getRouting().getXmlTypes().get(xmlType);
        
        if (xmlTypeConfig != null && xmlTypeConfig.isEnabled()) {
            System.out.println("   → Routing to configured channel: " + xmlTypeConfig.getChannel());
            return xmlTypeConfig.getChannel();
        }
        
        // Fallback to generic if configured
        var genericConfig = spiAppProperties.getRouting().getXmlTypes().get("generic");
        if (genericConfig != null && genericConfig.isEnabled()) {
            System.out.println("   → Routing to generic channel (fallback): " + genericConfig.getChannel());
            return genericConfig.getChannel();
        }
        
        System.out.println("   → No valid routing found, using default");
        return "genericProcessingChannel";
    }

    // =================== DYNAMIC TRANSFORMERS ===================
    
    @Bean
    @ConditionalOnProperty(name = "spiapp.channels.content-router.enabled", havingValue = "true")
    public DynamicTransformerFactory dynamicTransformerFactory() {
        return new DynamicTransformerFactory();
    }

    public class DynamicTransformerFactory {
        
        public String transformXml(String xmlPayload, String xmlType) {
            System.out.println("🔄 DYNAMIC TRANSFORMER: Processing " + xmlType.toUpperCase() + " XML");
            
            // Check if specialized service exists
            if (transformationService != null) {
                return callSpecializedTransformer(xmlPayload, xmlType);
            }
            
            // Fallback to basic transformation
            return transformWithMappers(xmlPayload, xmlType);
        }
        
        private String callSpecializedTransformer(String xmlPayload, String xmlType) {
            try {
                switch (xmlType.toLowerCase()) {
                    case "customer":
                        return transformationService.transformCustomerXmlToJson(xmlPayload);
                    case "order":
                        return transformationService.transformOrderXmlToJson(xmlPayload);
                    case "product":
                        return transformationService.transformProductXmlToJson(xmlPayload);
                    default:
                        return transformationService.transformGenericXmlToJson(xmlPayload);
                }
            } catch (Exception e) {
                System.err.println("❌ Specialized transformer failed: " + e.getMessage());
                return transformWithMappers(xmlPayload, xmlType);
            }
        }
    }

    // =================== CONFIGURABLE OUTPUT HANDLERS WITH PUB/SUB ===================
    
    public void handleOutput(String jsonResult, String xmlType) {
        System.out.println("🎯 === " + xmlType.toUpperCase() + " PROCESSING COMPLETE ===");
        System.out.println("📄 Result: " + jsonResult);
        
        // Publish to subscribers if pub/sub is enabled
        if (spiAppProperties.getPubsub().getMessageBroker().isEnabled() && pubSubService != null) {
            var xmlTypeConfig = spiAppProperties.getRouting().getXmlTypes().get(xmlType.toLowerCase());
            if (xmlTypeConfig != null && xmlTypeConfig.getSubscribers() != null) {
                pubSubService.publishToSubscribers(xmlType, jsonResult, xmlTypeConfig.getSubscribers());
            }
        }
        
        System.out.println("=" + "=".repeat(xmlType.length() + 35));
    }

    // =================== HELPER METHODS ===================
    
    private ChannelInterceptor createLoggingInterceptor(String channelName) {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (spiAppProperties.getProcessing().isEnableTracing()) {
                    System.out.println("📨 " + channelName + " - Processing: " + 
                        message.getPayload().toString().substring(0, Math.min(100, message.getPayload().toString().length())) + "...");
                }
                return message;
            }
        };
    }
    
    private String transformWithMappers(String xmlPayload, String type) {
        try {
            var jsonNode = xmlMapper.readTree(xmlPayload);
            String jsonData = objectMapper.writeValueAsString(jsonNode);
            
            return String.format(
                "{ \"type\": \"%s\", \"timestamp\": \"%d\", \"processor\": \"ConfigurableTransformer\", \"status\": \"success\", \"data\": %s }", 
                type, System.currentTimeMillis(), jsonData
            );
        } catch (Exception e) {
            return String.format(
                "{ \"type\": \"%s\", \"timestamp\": \"%d\", \"processor\": \"ConfigurableTransformer\", \"status\": \"error\", \"error\": \"%s\" }", 
                type, System.currentTimeMillis(), e.getMessage()
            );
        }
    }

    // =================== ERROR HANDLING ===================
    
    @Bean
    @ConditionalOnProperty(name = "spiapp.error-handling.dead-letter-queue", havingValue = "true")
    public MessageChannel errorChannel() {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                System.err.println("❌ ERROR CHANNEL: " + message.getPayload());
                return message;
            }
        });
        return channel;
    }

    @ServiceActivator(inputChannel = "errorChannel")
    @ConditionalOnProperty(name = "spiapp.error-handling.dead-letter-queue", havingValue = "true")
    public void handleError(Message<?> errorMessage) {
        System.err.println("🚨 DEAD LETTER QUEUE: Processing failed message");
        System.err.println("📧 Message: " + errorMessage.getPayload());
        System.err.println("🏷️  Headers: " + errorMessage.getHeaders());
        
        // Could implement retry logic here based on configuration
        if (spiAppProperties.getErrorHandling().getMaxRetryAttempts() > 0) {
            System.err.println("🔄 Retry attempts remaining: " + spiAppProperties.getErrorHandling().getMaxRetryAttempts());
        }
    }
}