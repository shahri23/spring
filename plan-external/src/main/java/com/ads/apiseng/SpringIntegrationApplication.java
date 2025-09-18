package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource({
     "file:./spi-config.xml"
})
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Spring Integration EIP Demo...");
        
        // DISABLE XML SCHEMA VALIDATION - Critical for OpenShift
        System.setProperty("spring.xml.ignore-schema-location", "true");
        System.setProperty("javax.xml.validation.SchemaFactory.disableCoreValidation", "true");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", 
                          "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        
        // Set JVM options for stability
        System.setProperty("java.awt.headless", "true");
        System.setProperty("spring.backgroundpreinitializer.ignore", "true");
        
        SpringApplication app = new SpringApplication(SpringIntegrationApplication.class);
        app.run(args);
        
        System.out.println("✅ Application started successfully!");
        System.out.println("📖 Test with: curl -X POST http://localhost:8080/api/basic-transform -d 'Hello World'");
    }
}