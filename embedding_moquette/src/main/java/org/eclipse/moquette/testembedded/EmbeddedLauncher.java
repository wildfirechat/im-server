package org.eclipse.moquette.testembedded;

import org.eclipse.moquette.server.ClasspathConfig;
import org.eclipse.moquette.server.IConfig;
import org.eclipse.moquette.server.Server;

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