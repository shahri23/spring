 
package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class SpiappApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpiappApplication.class, args);
        System.out.println("SpiApp started successfully on port 8080");
        System.out.println("API endpoint: POST http://localhost:8080/api/transform");
    }
}
