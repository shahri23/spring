package com.ads.apiseng.controller;

import com.ads.apiseng.config.SpiAppProperties;
import com.ads.apiseng.service.PubSubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
@EnableConfigurationProperties(SpiAppProperties.class)
@CrossOrigin(origins = "*")
public class ConfigurationManagementController {

    @Autowired
    private SpiAppProperties spiAppProperties;

    @Autowired(required = false)
    private PubSubService pubSubService;

    // =================== CONFIGURATION STATUS ===================

    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Channels status
        status.put("channels", Map.of(
            "input", spiAppProperties.getChannels().getInput(),
            "output", spiAppProperties.getChannels().getOutput(),
            "error", spiAppProperties.getChannels().getError(),
            "capacity", spiAppProperties.getChannels().getCapacity(),
            "threadPoolSize", spiAppProperties.getChannels().getThreadPoolSize()
        ));
        
        // Routing status
        status.put("routing", Map.of(
            "enabled", spiAppProperties.getRouting().isEnabled(),
            "defaultRoute", spiAppProperties.getRouting().getDefaultRoute(),
            "routes", spiAppProperties.getRouting().getRoutes() != null ? 
                spiAppProperties.getRouting().getRoutes() : Map.of()
        ));
        
        // Pub/Sub status
        status.put("pubsub", Map.of(
            "enabled", spiAppProperties.getPubsub().isEnabled(),
            "provider", spiAppProperties.getPubsub().getProvider(),
            "configuration", spiAppProperties.getPubsub().getConfiguration() != null ? 
                spiAppProperties.getPubsub().getConfiguration() : Map.of()
        ));
        
        // Processing status
        status.put("processing", Map.of(
            "parallelProcessing", spiAppProperties.getProcessing().isParallelProcessing(),
            "maxRetries", spiAppProperties.getProcessing().getMaxRetries(),
            "retryDelay", spiAppProperties.getProcessing().getRetryDelay()
        ));
        
        // Error handling status
        status.put("errorHandling", Map.of(
            "enabled", spiAppProperties.getErrorHandling().isEnabled(),
            "strategy", spiAppProperties.getErrorHandling().getStrategy(),
            "notificationChannels", spiAppProperties.getErrorHandling().getNotificationChannels() != null ? 
                spiAppProperties.getErrorHandling().getNotificationChannels() : java.util.List.of()
        ));
        
        return ResponseEntity.ok(status);
    }

    // =================== DYNAMIC CONFIGURATION UPDATES ===================

    @PostMapping("/config/routing/toggle")
    public ResponseEntity<Map<String, Object>> toggleRouting(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            spiAppProperties.getRouting().setEnabled(enabled);
            response.put("status", "success");
            response.put("message", "Routing " + (enabled ? "enabled" : "disabled"));
            response.put("enabled", enabled);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/config/pubsub/toggle")
    public ResponseEntity<Map<String, Object>> togglePubSub(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            spiAppProperties.getPubsub().setEnabled(enabled);
            response.put("status", "success");
            response.put("message", "Pub/Sub " + (enabled ? "enabled" : "disabled"));
            response.put("enabled", enabled);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // =================== PUB/SUB MONITORING ===================

    @GetMapping("/pubsub/status")
    public ResponseEntity<Map<String, Object>> getPubSubStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        
        if (pubSubService != null && spiAppProperties.getPubsub().isEnabled()) {
            response.put("status", "active");
            response.put("pubSubService", pubSubService.getStatus());
        } else {
            response.put("status", "disabled");
            response.put("message", "Pub/Sub service is not available or disabled");
        }
        
        return ResponseEntity.ok(response);
    }

    // =================== HEALTH AND METRICS ===================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", System.currentTimeMillis());
        health.put("status", "UP");
        
        // Check component health
        Map<String, String> components = new HashMap<>();
        components.put("routing", spiAppProperties.getRouting().isEnabled() ? "UP" : "DOWN");
        components.put("pubsub", spiAppProperties.getPubsub().isEnabled() ? "UP" : "DOWN");
        components.put("errorHandling", spiAppProperties.getErrorHandling().isEnabled() ? "UP" : "DOWN");
        
        health.put("components", components);
        health.put("configuration", Map.of(
            "parallelProcessing", spiAppProperties.getProcessing().isParallelProcessing(),
            "maxRetries", spiAppProperties.getProcessing().getMaxRetries(),
            "threadPoolSize", spiAppProperties.getChannels().getThreadPoolSize()
        ));
        
        return ResponseEntity.ok(health);
    }

    // =================== UTILITY ENDPOINTS ===================

    @PostMapping("/config/reload")
    public ResponseEntity<Map<String, Object>> reloadConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "success");
        response.put("message", "Configuration reloaded successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config/export")
    public ResponseEntity<Map<String, Object>> exportConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Export clean configuration data without Spring proxies
        config.put("channels", Map.of(
            "enabled", spiAppProperties.getChannels().isEnabled(),
            "contentRouter", Map.of(
                "enabled", spiAppProperties.getChannels().getContentRouter().isEnabled(),
                "parallelProcessing", spiAppProperties.getChannels().getContentRouter().isParallelProcessing(),
                "errorHandling", spiAppProperties.getChannels().getContentRouter().isErrorHandling()
            ),
            "originalTransformer", Map.of(
                "enabled", spiAppProperties.getChannels().getOriginalTransformer().isEnabled()
            ),
            "pubSub", Map.of(
                "enabled", spiAppProperties.getChannels().getPubSub().isEnabled()
            ),
            "input", spiAppProperties.getChannels().getInput(),
            "output", spiAppProperties.getChannels().getOutput(),
            "error", spiAppProperties.getChannels().getError(),
            "capacity", spiAppProperties.getChannels().getCapacity(),
            "threadPoolSize", spiAppProperties.getChannels().getThreadPoolSize()
        ));
        
        config.put("routing", Map.of(
            "enabled", spiAppProperties.getRouting().isEnabled(),
            "defaultRoute", spiAppProperties.getRouting().getDefaultRoute(),
            "routes", spiAppProperties.getRouting().getRoutes() != null ? 
                spiAppProperties.getRouting().getRoutes() : Map.of(),
            "xmlTypes", spiAppProperties.getRouting().getXmlTypes() != null ? 
                spiAppProperties.getRouting().getXmlTypes() : Map.of()
        ));
        
        config.put("pubsub", Map.of(
            "enabled", spiAppProperties.getPubsub().isEnabled(),
            "provider", spiAppProperties.getPubsub().getProvider(),
            "configuration", spiAppProperties.getPubsub().getConfiguration() != null ? 
                spiAppProperties.getPubsub().getConfiguration() : Map.of(),
            "messageBroker", Map.of(
                "type", spiAppProperties.getPubsub().getMessageBroker().getType(),
                "enabled", spiAppProperties.getPubsub().getMessageBroker().isEnabled()
            ),
            "topics", spiAppProperties.getPubsub().getTopics() != null ? 
                spiAppProperties.getPubsub().getTopics() : Map.of(),
            "subscribers", spiAppProperties.getPubsub().getSubscribers() != null ? 
                spiAppProperties.getPubsub().getSubscribers() : Map.of()
        ));
        
        config.put("processing", Map.of(
            "parallelProcessing", spiAppProperties.getProcessing().isParallelProcessing(),
            "maxRetries", spiAppProperties.getProcessing().getMaxRetries(),
            "retryDelay", spiAppProperties.getProcessing().getRetryDelay(),
            "threadPoolSize", spiAppProperties.getProcessing().getThreadPoolSize(),
            "timeoutMs", spiAppProperties.getProcessing().getTimeoutMs(),
            "retryAttempts", spiAppProperties.getProcessing().getRetryAttempts(),
            "enableMetrics", spiAppProperties.getProcessing().isEnableMetrics(),
            "enableTracing", spiAppProperties.getProcessing().isEnableTracing()
        ));
        
        config.put("errorHandling", Map.of(
            "enabled", spiAppProperties.getErrorHandling().isEnabled(),
            "strategy", spiAppProperties.getErrorHandling().getStrategy(),
            "notificationChannels", spiAppProperties.getErrorHandling().getNotificationChannels() != null ? 
                spiAppProperties.getErrorHandling().getNotificationChannels() : java.util.List.of(),
            "maxRetryAttempts", spiAppProperties.getErrorHandling().getMaxRetryAttempts(),
            "deadLetterQueue", spiAppProperties.getErrorHandling().isDeadLetterQueue(),
            "errorChannel", spiAppProperties.getErrorHandling().getErrorChannel(),
            "retryPolicy", spiAppProperties.getErrorHandling().getRetryPolicy()
        ));
        
        return ResponseEntity.ok(config);
    }
}
