package com.ads.apiseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import java.io.File;

@SpringBootApplication
public class SpringIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringIntegrationApplication.class);

        // Dynamically import all XML files from ./tmp
        app.addInitializers((ApplicationContextInitializer<ConfigurableApplicationContext>) ctx -> {
            if (ctx instanceof GenericApplicationContext gac) {
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(gac);
                File tmpDir = new File("./tmp");
                if (tmpDir.exists() && tmpDir.isDirectory()) {
                    File[] files = tmpDir.listFiles((dir, name) -> name.endsWith(".xml"));
                    if (files != null) {
                        for (File f : files) {
                            System.out.println("📂 Importing flow: " + f.getAbsolutePath());
                            reader.loadBeanDefinitions("file:" + f.getAbsolutePath());
                        }
                    }
                } else {
                    System.out.println("⚠️ No ./tmp directory found — skipping external flows.");
                }
            }
        });

        app.run(args);
    }
}
