package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
        System.out.println("===============================================");
        System.out.println("Spring Integration Demo App started.");
        System.out.println("Looking for external config in /tmp/integration.xml");
        System.out.println("If not present, no flows will be active.");
        System.out.println("===============================================");
    }
}
