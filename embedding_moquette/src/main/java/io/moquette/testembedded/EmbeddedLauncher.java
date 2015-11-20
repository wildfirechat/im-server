package io.moquette.testembedded;

import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.ClasspathConfig;

import java.io.IOException;

public class EmbeddedLauncher {
    public static void main(String[] args) throws InterruptedException, IOException {
        final IConfig classPathConfig = new ClasspathConfig();

        final Server mqttBroker = new Server();
        mqttBroker.startServer(classPathConfig);
        System.out.println("Broker started");
        Thread.sleep(1000);
        System.out.println("Stopping broker");
        mqttBroker.stopServer();
        System.out.println("Broker stopped");
    }
}