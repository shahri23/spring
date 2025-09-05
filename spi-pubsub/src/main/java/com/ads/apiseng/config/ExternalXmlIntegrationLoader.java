package com.ads.apiseng.config;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

import java.io.File;

/**
 * Loads external Spring XML bean definitions from /tmp if present.
 * This enables dropping in a Spring Integration XML config for demo purposes.
 * If no supported file is found, the app falls back to the Java DSL config.
 */
public class ExternalXmlIntegrationLoader implements ApplicationContextInitializer<GenericApplicationContext> {

    private static final String[] CANDIDATES = new String[] {
            "/tmp/integration.xml",
            "/tmp/integration-context.xml",
            "/tmp/spring-integration.xml"
    };

    @Override
    public void initialize(GenericApplicationContext context) {
        for (String path : CANDIDATES) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
                int count = reader.loadBeanDefinitions("file:" + f.getAbsolutePath());
                System.out.println("[ExternalXmlIntegrationLoader] Loaded " + count + " bean definitions from " + f.getAbsolutePath());
                return;
            }
        }
        System.out.println("[ExternalXmlIntegrationLoader] No external XML found in /tmp. Using in-jar Java DSL config.");
    }
}
