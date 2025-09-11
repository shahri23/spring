package com.ads.apiseng.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/")
    public String home() {
        return "Spring Integration Application is running!";
    }

    @PostMapping("/test")
    public String test(@RequestBody String message) {
        return "Received: " + message.toUpperCase();
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}