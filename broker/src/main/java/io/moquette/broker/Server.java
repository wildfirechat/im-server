package io.moquette.broker;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.config.FileResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.security.AcceptAllAuthenticator;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.moquette.spi.security.ISslContextCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.moquette.logging.LoggingUtils.getInterceptorIds;

public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(io.moquette.server.Server.class);

    private ScheduledExecutorService scheduler;
    private NewNettyAcceptor acceptor;
    private volatile boolean initialized;

    public static void main(String[] args) throws IOException {
        final Server server = new Server();
        server.startServer();
        System.out.println("Server started, version 0.12-SNAPSHOT");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
    }

    /**
     * Starts Moquette bringing the configuration from the file located at m_config/moquette.conf
     *
     * @throws IOException in case of any IO error.
     */
    public void startServer() throws IOException {
        File defaultConfigurationFile = defaultConfigFile();
        LOG.info("Starting Moquette server. Configuration file path={}", defaultConfigurationFile.getAbsolutePath());
        IResourceLoader filesystemLoader = new FileResourceLoader(defaultConfigurationFile);
        final IConfig config = new ResourceLoaderConfig(filesystemLoader);
        startServer(config);
    }

    public void startServer(IConfig config) throws IOException {
        LOG.debug("Starting Moquette server using IConfig instance");
        startServer(config, null);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers) throws IOException {
        LOG.debug("Starting moquette server using IConfig instance and intercept handlers");
        startServer(config, handlers, null, null, null);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers, ISslContextCreator sslCtxCreator,
                            IAuthenticator authenticator, IAuthorizatorPolicy authorizatorPolicy) throws IOException {
        final long start = System.currentTimeMillis();
        if (handlers == null) {
            handlers = Collections.emptyList();
        }
        LOG.trace("Starting Moquette Server. MQTT message interceptors={}", getInterceptorIds(handlers));

        scheduler = Executors.newScheduledThreadPool(1);

        final String handlerProp = System.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (handlerProp != null) {
            config.setProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME, handlerProp);
        }
        final String persistencePath = config.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME);
        LOG.debug("Configuring Using persistent store file, path={}", persistencePath);
//        m_processorBootstrapper = new ProtocolProcessorBootstrapper();
//        final ProtocolProcessor processor = m_processorBootstrapper.init(config, handlers, authenticator, authorizator,
//            this);
        LOG.debug("Initialized MQTT protocol processor");
//        if (sslCtxCreator == null) {
//            LOG.info("Using default SSL context creator");
//            sslCtxCreator = new DefaultMoquetteSslContextCreator(config);
//        }
        // DBG
        if (authenticator == null) {
            authenticator = new AcceptAllAuthenticator();
        }
        if (authorizatorPolicy == null) {
            authorizatorPolicy = new PermitAllAuthorizatorPolicy();
        }

        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        ISessionsStore sessionStore = memStorage.sessionsStore();
        SessionsRepository sessionsRepository = new SessionsRepository(sessionStore, null);
        // DBG
        
        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        subscriptions.init(sessionsRepository);
        SessionRegistry sessions = new SessionRegistry(subscriptions);
        final PostOffice dispatcher = new PostOffice(subscriptions, authorizatorPolicy, new MemoryRetainedRepository(), sessions);
        final BrokerConfiguration brokerConfig = new BrokerConfiguration(config);
        MQTTConnectionFactory connectionFactory = new MQTTConnectionFactory(brokerConfig, authenticator, sessions,
                                                                            dispatcher);

        final NewNettyMQTTHandler mqttHandler = new NewNettyMQTTHandler(connectionFactory);
        acceptor = new NewNettyAcceptor();
        acceptor.initialize(mqttHandler, config, sslCtxCreator);

        final long startTime = System.currentTimeMillis() - start;
        LOG.info("Moquette server has been started successfully in {} ms", startTime);
        initialized = true;
    }

    public void stopServer() {
        LOG.info("Unbinding server from the configured ports");
        acceptor.close();
        LOG.trace("Stopping MQTT protocol processor");
//        m_processorBootstrapper.shutdown();
        initialized = false;

        // calling shutdown() does not actually stop tasks that are not cancelled,
        // and SessionsRepository does not stop its tasks. Thus shutdownNow().
        scheduler.shutdownNow();

        LOG.info("Moquette server has been stopped.");
    }

    private static File defaultConfigFile() {
        String configPath = System.getProperty("moquette.path", null);
        return new File(configPath, IConfig.DEFAULT_CONFIG);
    }
}
