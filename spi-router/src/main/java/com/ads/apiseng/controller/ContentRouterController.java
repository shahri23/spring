package com.ads.apiseng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.*;
import com.ads.apiseng.util.XmlTypeDetector;

@RestController
@RequestMapping("/api/content-router")
@CrossOrigin(origins = "*")
public class ContentRouterController {

    @Autowired
    private MessagingTemplate messagingTemplate;

    @PostMapping("/route-xml")
    public ResponseEntity<String> routeXml(@RequestBody String xmlContent) {
        try {
            // Validate XML
            if (!XmlTypeDetector.isValidXml(xmlContent)) {
                return ResponseEntity.badRequest()
                    .body("Invalid XML format provided");
            }

            // Detect XML type for logging
            String xmlType = XmlTypeDetector.detectType(xmlContent);
            System.out.println("Routing XML of type: " + xmlType);

            Message<String> message = MessageBuilder
                .withPayload(xmlContent)
                .setHeader("xml-type", xmlType)
                .setHeader("processing-time", System.currentTimeMillis())
                .build();
                
            messagingTemplate.send("contentRouterInputChannel", message);
            
            return ResponseEntity.ok(String.format(
                "{ \"status\": \"success\", \"message\": \"XML successfully routed and processed\", \"detectedType\": \"%s\", \"timestamp\": \"%d\" }", 
                xmlType, System.currentTimeMillis()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(String.format(
                    "{ \"status\": \"error\", \"message\": \"Error processing XML: %s\", \"timestamp\": \"%d\" }", 
                    e.getMessage(), System.currentTimeMillis()));
        }
    }

    @PostMapping("/route-customer")
    public ResponseEntity<String> routeCustomerXml(@RequestBody String customerXml) {
        return routeSpecificXml(customerXml, "customer");
    }

    @PostMapping("/route-order")
    public ResponseEntity<String> routeOrderXml(@RequestBody String orderXml) {
        return routeSpecificXml(orderXml, "order");
    }
    
    @PostMapping("/route-product")
    public ResponseEntity<String> routeProductXml(@RequestBody String productXml) {
        return routeSpecificXml(productXml, "product");
    }

    private ResponseEntity<String> routeSpecificXml(String xmlContent, String expectedType) {
        try {
            if (!XmlTypeDetector.isValidXml(xmlContent)) {
                return ResponseEntity.badRequest()
                    .body(String.format("Invalid XML format for %s", expectedType));
            }

            String detectedType = XmlTypeDetector.detectType(xmlContent);
            
            Message<String> message = MessageBuilder
                .withPayload(xmlContent)
                .setHeader("xml-type", detectedType)
                .setHeader("expected-type", expectedType)
                .setHeader("processing-time", System.currentTimeMillis())
                .build();
                
            messagingTemplate.send("contentRouterInputChannel", message);
            
            return ResponseEntity.ok(String.format(
                "{ \"status\": \"success\", \"message\": \"%s XML processed\", \"expectedType\": \"%s\", \"detectedType\": \"%s\", \"timestamp\": \"%d\" }", 
                expectedType.toUpperCase(), expectedType, detectedType, System.currentTimeMillis()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(String.format(
                    "{ \"status\": \"error\", \"message\": \"Error processing %s XML: %s\", \"timestamp\": \"%d\" }", 
                    expectedType, e.getMessage(), System.currentTimeMillis()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(String.format(
            "{ \"status\": \"UP\", \"service\": \"Content-Based Router\", \"timestamp\": \"%d\", \"message\": \"Content-Based Router is running successfully!\" }", 
            System.currentTimeMillis()));
    }

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok(
            "{ \"service\": \"Content-Based Router\", " +
            "\"description\": \"Enterprise Integration Pattern - Routes XML messages based on content type\", " +
            "\"supportedTypes\": [\"CUSTOMER\", \"ORDER\", \"PRODUCT\", \"GENERIC\"], " +
            "\"endpoints\": [\"/route-xml\", \"/route-customer\", \"/route-order\", \"/route-product\"], " +
            "\"version\": \"1.0.0\" }"
        );
    }

    @PostMapping("/test-detection")
    public ResponseEntity<String> testXmlTypeDetection(@RequestBody String xmlContent) {
        try {
            boolean isValid = XmlTypeDetector.isValidXml(xmlContent);
            String detectedType = XmlTypeDetector.detectType(xmlContent);
            String rootElement = XmlTypeDetector.extractRootElement(xmlContent);
            
            return ResponseEntity.ok(String.format(
                "{ \"isValidXml\": %b, \"detectedType\": \"%s\", \"rootElement\": \"%s\", \"timestamp\": \"%d\" }", 
                isValid, detectedType, rootElement, System.currentTimeMillis()));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(String.format(
                    "{ \"error\": \"Error analyzing XML: %s\", \"timestamp\": \"%d\" }", 
                    e.getMessage(), System.currentTimeMillis()));
        }
    }
}