package com.ads.apiseng.config;

import com.ads.apiseng.handlers.SimpleHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@EnableIntegration
@RestController
public class QuickIntegrationConfig {

    // Simple REST endpoint as backup
    @PostMapping("/api/basic-transform")
    public String basicTransform(@RequestBody String payload) {
        System.out.println("[BASIC-PROCESSOR] received: " + payload);
        String result = "PROCESSED: " + payload.toUpperCase();
        System.out.println("[BASIC-PROCESSOR] sending: " + result);
        return result;
    }

    // Also test endpoint
    @PostMapping("/api/quick-test")
    public String test(@RequestBody String payload) {
        return "✅ Test successful: " + payload.toUpperCase();
    }
}