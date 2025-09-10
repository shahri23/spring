package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApplication.class, args);
        System.out.println("===============================================");
        System.out.println("🚀 Spring Integration EIP Demo App Started");
        System.out.println("===============================================");
        System.out.println("📁 Loading EIP configurations from:");
        System.out.println("   ✅ ./tmp/eip-basic-transforms.xml");
        // System.out.println("   ✅ ./tmp/eip-routing-pubsub.xml");
        // System.out.println("   ✅ ./tmp/eip-advanced-patterns.xml");
        System.out.println("===============================================");
        System.out.println("🌐 HTTP Endpoints Available:");
        System.out.println("   POST /api/basic-transform     - Basic transformations");
        System.out.println("   POST /api/orders              - JSON order processing");
        System.out.println("   POST /api/inventory           - XML inventory processing");
        System.out.println("   POST /api/batch               - CSV batch processing");
        System.out.println("   POST /api/advanced/aggregate  - Message aggregation");
        System.out.println("   POST /api/advanced/scatter-gather - Parallel processing");
        System.out.println("===============================================");
        System.out.println("🔍 Monitor logs to see EIP patterns in action!");
        System.out.println("===============================================");
    }
}