package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;


@SpringBootApplication
@ImportResource({
    // "file:./tmp/eip-basic-transforms.xml",
    // "file:./tmp/eip-routing-pubsub.xml",
    "file:./tmp/eip-advanced-patterns.xml"
    // "file:./tmp/combined.xml"
})
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Spring Integration EIP Demo...");
        
        // Set JVM options for stability
        System.setProperty("java.awt.headless", "true");
        System.setProperty("spring.backgroundpreinitializer.ignore", "true");
        
        SpringApplication app = new SpringApplication(SpringIntegrationApplication.class);
        app.run(args);
        
        System.out.println("✅ Application started successfully!");
        System.out.println("📖 Check README.md for testing instructions");
    }
}