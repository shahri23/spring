 
package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class SpringIntegrationApplication {
    public static void main(String[] args) {
        // Ensure Spring reads YAML/properties from /tmp if present (and ignores if missing)
        System.setProperty("spring.config.import", "optional:file:/tmp/");
        System.setProperty("spring.config.additional-location", "optional:file:/tmp/");
        
        SpringApplication app = new SpringApplication(SpringIntegrationApplication.class);
        // Load external XML Spring Integration context from /tmp if present
        app.addInitializers(new com.ads.apiseng.config.ExternalXmlIntegrationLoader());
        app.run(args);

        System.out.println("Spring Integration App (spi-app) started successfully on port 8080");
        System.out.println("API endpoint: POST http://localhost:8080/api/transform");
        System.out.println("Health check: GET http://localhost:8080/api/management/health");
        System.out.println("External config: looks for /tmp/application.yml and /tmp/integration.xml (optional).");
    }
}
