package com.ads.apiseng;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebugExitConfig {

    @Bean
    public ExitCodeGenerator debugExitCodeGenerator() {
        return () -> {
            System.err.println("🔍 ExitCodeGenerator invoked — stack trace:");
            new Exception("ExitCodeGenerator stack").printStackTrace(System.err);
            return 42;
        };
    }
}
