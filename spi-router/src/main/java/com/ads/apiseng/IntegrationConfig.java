package com.ads.apiseng;

import com.ads.apiseng.service.XmlTransformationService;
import com.ads.apiseng.util.XmlTypeDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;

@Configuration
@EnableIntegration
public class IntegrationConfig {

    @Autowired(required = false)
    private XmlTransformationService transformationService;

    // =================== SHARED MAPPERS ===================
    
    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessagingTemplate messagingTemplate() {
        return new MessagingTemplate();
    }

    // =================== ORIGINAL XML→JSON TRANSFORMER ===================
    
    @Bean
    public MessageChannel xmlInputChannel() {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                System.out.println("📨 Original XML Input Channel - Processing: " + message.getPayload());
                return message;
            }
        });
        return channel;
    }

    @ServiceActivator(inputChannel = "xmlInputChannel")
    @Bean
    public XmlToJsonTransformer xmlToJsonTransformer() {
        System.out.println("🔧 Creating Original XML→JSON Transformer");
        return new XmlToJsonTransformer(xmlMapper(), objectMapper());
    }

    // =================== CONTENT-BASED ROUTER CHANNELS ===================
    
    @Bean
    public MessageChannel contentRouterInputChannel() {
        DirectChannel channel = new DirectChannel();
        channel.addInterceptor(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                System.out.println("📨 Content Router Input Channel - Received: " + message.getPayload());
                return message;
            }
        });
        return channel;
    }

    @Bean
    public MessageChannel customerProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel orderProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel productProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel genericProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel customerOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel orderOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel productOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel genericOutputChannel() {
        return new DirectChannel();
    }

    // =================== CONTENT-BASED ROUTER ===================
    
    @Router(inputChannel = "contentRouterInputChannel")
    public String routeXmlByContent(String xmlPayload) {
        String xmlType = XmlTypeDetector.detectType(xmlPayload);
        System.out.println("🔀 CONTENT-BASED ROUTER: Detected Type = " + xmlType);
        
        switch (xmlType) {
            case "CUSTOMER":
                System.out.println("   → Routing to CUSTOMER processing channel");
                return "customerProcessingChannel";
            case "ORDER":
                System.out.println("   → Routing to ORDER processing channel");
                return "orderProcessingChannel";
            case "PRODUCT":
                System.out.println("   → Routing to PRODUCT processing channel");
                return "productProcessingChannel";
            default:
                System.out.println("   → Routing to GENERIC processing channel (type: " + xmlType + ")");
                return "genericProcessingChannel";
        }
    }

    // =================== CONTENT-BASED TRANSFORMERS ===================
    
    @Transformer(inputChannel = "customerProcessingChannel", outputChannel = "customerOutputChannel")
    public String transformCustomerXml(String xmlPayload) {
        System.out.println("🔄 CUSTOMER TRANSFORMER: Processing specialized customer XML");
        if (transformationService != null) {
            return transformationService.transformCustomerXmlToJson(xmlPayload);
        } else {
            // Fallback to basic transformation
            return transformWithMappers(xmlPayload, "customer");
        }
    }

    @Transformer(inputChannel = "orderProcessingChannel", outputChannel = "orderOutputChannel")
    public String transformOrderXml(String xmlPayload) {
        System.out.println("🔄 ORDER TRANSFORMER: Processing specialized order XML");
        if (transformationService != null) {
            return transformationService.transformOrderXmlToJson(xmlPayload);
        } else {
            return transformWithMappers(xmlPayload, "order");
        }
    }

    @Transformer(inputChannel = "productProcessingChannel", outputChannel = "productOutputChannel")
    public String transformProductXml(String xmlPayload) {
        System.out.println("🔄 PRODUCT TRANSFORMER: Processing specialized product XML");
        if (transformationService != null) {
            return transformationService.transformProductXmlToJson(xmlPayload);
        } else {
            return transformWithMappers(xmlPayload, "product");
        }
    }

    @Transformer(inputChannel = "genericProcessingChannel", outputChannel = "genericOutputChannel")
    public String transformGenericXml(String xmlPayload) {
        System.out.println("🔄 GENERIC TRANSFORMER: Processing generic XML");
        if (transformationService != null) {
            return transformationService.transformGenericXmlToJson(xmlPayload);
        } else {
            return transformWithMappers(xmlPayload, "generic");
        }
    }

    // =================== CONTENT-BASED OUTPUT HANDLERS ===================
    
    @ServiceActivator(inputChannel = "customerOutputChannel")
    public void handleCustomerOutput(String jsonResult) {
        System.out.println("🎯 === CUSTOMER PROCESSING COMPLETE ===");
        System.out.println("📄 Result: " + jsonResult);
        System.out.println("==========================================");
    }

    @ServiceActivator(inputChannel = "orderOutputChannel")
    public void handleOrderOutput(String jsonResult) {
        System.out.println("🎯 === ORDER PROCESSING COMPLETE ===");
        System.out.println("📄 Result: " + jsonResult);
        System.out.println("====================================");
    }

    @ServiceActivator(inputChannel = "productOutputChannel")
    public void handleProductOutput(String jsonResult) {
        System.out.println("🎯 === PRODUCT PROCESSING COMPLETE ===");
        System.out.println("📄 Result: " + jsonResult);
        System.out.println("======================================");
    }

    @ServiceActivator(inputChannel = "genericOutputChannel")
    public void handleGenericOutput(String jsonResult) {
        System.out.println("🎯 === GENERIC PROCESSING COMPLETE ===");
        System.out.println("📄 Result: " + jsonResult);
        System.out.println("======================================");
    }

    // =================== HELPER METHOD ===================
    
    private String transformWithMappers(String xmlPayload, String type) {
        try {
            // Use your existing mappers for fallback transformation
            var jsonNode = xmlMapper().readTree(xmlPayload);
            String jsonData = objectMapper().writeValueAsString(jsonNode);
            
            return String.format(
                "{ \"type\": \"%s\", \"timestamp\": \"%d\", \"processor\": \"FallbackTransformer\", \"status\": \"success\", \"data\": %s }", 
                type, System.currentTimeMillis(), jsonData
            );
        } catch (Exception e) {
            return String.format(
                "{ \"type\": \"%s\", \"timestamp\": \"%d\", \"processor\": \"FallbackTransformer\", \"status\": \"error\", \"error\": \"%s\" }", 
                type, System.currentTimeMillis(), e.getMessage()
            );
        }
    }
}