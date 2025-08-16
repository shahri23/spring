 
package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class SpringIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
        System.out.println("Spring Integration App (spi-app) started successfully on port 8080");
        System.out.println("API endpoint: POST http://localhost:8080/api/transform");
        System.out.println("Health check: GET http://localhost:8080/api/management/health");
    }
}
