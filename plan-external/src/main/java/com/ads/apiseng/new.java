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
        
        // DISABLE ALL XML VALIDATION - Multiple approaches
        System.setProperty("spring.xml.ignore-schema-location", "true");
        System.setProperty("spring.xml.ignore-dtd", "true");
        System.setProperty("javax.xml.parsers.SAXParserFactory", 
                          "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", 
                          "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        System.setProperty("javax.xml.validation.SchemaFactory", 
                          "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory");
        
        // Additional XML processing properties
        System.setProperty("org.xml.sax.driver", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
        System.setProperty("javax.xml.accessExternalDTD", "all");
        System.setProperty("javax.xml.accessExternalSchema", "all");
        
        // Set JVM options for stability
        System.setProperty("java.awt.headless", "true");
        System.setProperty("spring.backgroundpreinitializer.ignore", "true");
        
        SpringApplication app = new SpringApplication(SpringIntegrationApplication.class);
        
        // Add additional Spring properties programmatically
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        
        app.run(args);
        
        System.out.println("✅ Application started successfully!");
        System.out.println("📖 Test with: curl -X POST http://localhost:8080/api/basic-transform -d 'Hello World'");
    }
}