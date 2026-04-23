package com.focusdesk.app;

import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) {
        try {
            Class<?> appClass = Class.forName("com.focusdesk.app.App");
            Class<?> applicationClass = Class.forName("javafx.application.Application");
            Method launch = applicationClass.getMethod("launch", Class.class, String[].class);
            launch.invoke(null, appClass, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to launch JavaFX. Run with Maven (mvn javafx:run) so JavaFX dependencies are on the classpath.",
                    e
            );
        }
    }
}
