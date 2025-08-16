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
@RequestMapping("/api/router")
@EnableConfigurationProperties(SpiAppProperties.class)
@CrossOrigin(origins = "*")
public class ContentRouterController {

    @Autowired
    private SpiAppProperties spiAppProperties;

    @Autowired(required = false)
    private PubSubService pubSubService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        status.put("status", "UP");
        status.put("routing", Map.of(
            "enabled", spiAppProperties.getRouting().isEnabled(),
            "defaultRoute", spiAppProperties.getRouting().getDefaultRoute()
        ));
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/route")
    public ResponseEntity<Map<String, Object>> routeContent(@RequestBody Map<String, Object> content) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        
        if (!spiAppProperties.getRouting().isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Routing is disabled");
            return ResponseEntity.ok(response);
        }
        
        // Simple routing logic
        String route = spiAppProperties.getRouting().getDefaultRoute();
        response.put("status", "routed");
        response.put("route", route);
        response.put("content", content);
        
        return ResponseEntity.ok(response);
    }
}