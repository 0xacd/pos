package com.anymind.pos.config;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;

@Configuration
public class PropertyLogger {

    private final Environment env;

    public PropertyLogger(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logProperties() {
        if (env instanceof StandardEnvironment standardEnv) {
            PropertySources propertySources = standardEnv.getPropertySources();
            System.out.println("Loaded Properties:");
            for (PropertySource<?> propertySource : propertySources) {
                if (propertySource.getName().contains("Config resource")){
                    System.out.println("  *Property Source: " + propertySource.getName());
                    if (propertySource.getSource() instanceof java.util.Map) {
                        ((java.util.Map<?, ?>) propertySource.getSource()).forEach((key, value) -> {
                            if (!(key.equals("java.class.path") || key.equals("surefire.test.class.path"))) System.out.println("    --" + key + ": " + value);
                        });
                    }
                }
            }
        } else {
            System.out.println("The environment is not a StandardEnvironment instance.");
        }
    }
}
