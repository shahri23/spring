package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);

        System.out.println("=================================================");
        System.out.println("Spring Integration External XML Demo started.");
        System.out.println("Looking for /tmp/integration.xml");
        System.out.println("If missing, no flows will be active.");
        System.out.println("=================================================");
    }
}
