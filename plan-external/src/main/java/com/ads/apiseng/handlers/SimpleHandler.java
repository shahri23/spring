package com.ads.apiseng.handlers;

public class SimpleHandler {
    private final String name;

    public SimpleHandler(String name) {
        this.name = name;
    }

    public void handle(String message) {
        System.out.println("[" + name + "] received: " + message);
    }
}
