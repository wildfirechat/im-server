package io.moquette.broker;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.config.*;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.BrokerInterceptor;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.security.AcceptAllAuthenticator;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.moquette.spi.security.ISslContextCreator;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.moquette.logging.LoggingUtils.getInterceptorIds;

public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(io.moquette.broker.Server.class);

    private ScheduledExecutorService scheduler;
    private NewNettyAcceptor acceptor;
    private volatile boolean initialized;
    private PostOffice dispatcher;
    private BrokerInterceptor interceptor;

    public static void main(String[] args) throws IOException {
        final Server server = new Server();
        server.startServer();
        System.out.println("Server started, version 0.12-SNAPSHOT");
        //Bind a shutdown hook
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

    private static File defaultConfigFile() {
        String configPath = System.getProperty("moquette.path", null);
        return new File(configPath, IConfig.DEFAULT_CONFIG);
    }

    /**
     * Starts Moquette bringing the configuration from the given file
     *
     * @param configFile text file that contains the configuration.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(File configFile) throws IOException {
        LOG.info("Starting Moquette server. Configuration file path: {}", configFile.getAbsolutePath());
        IResourceLoader filesystemLoader = new FileResourceLoader(configFile);
        final IConfig config = new ResourceLoaderConfig(filesystemLoader);
        startServer(config);
    }

    /**
     * Starts the server with the given properties.
     * <p>
     * Its suggested to at least have the following properties:
     * <ul>
     *  <li>port</li>
     *  <li>password_file</li>
     * </ul>
     *
     * @param configProps the properties map to use as configuration.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(Properties configProps) throws IOException {
        LOG.debug("Starting Moquette server using properties object");
        final IConfig config = new MemoryConfig(configProps);
        startServer(config);
    }

    /**
     * Starts Moquette bringing the configuration files from the given Config implementation.
     *
     * @param config the configuration to use to start the broker.
     * @throws IOException in case of any IO Error.
     */
    public void startServer(IConfig config) throws IOException {
        LOG.debug("Starting Moquette server using IConfig instance");
        startServer(config, null);
    }

    /**
     * Starts Moquette with config provided by an implementation of IConfig class and with the set
     * of InterceptHandler.
     *
     * @param config   the configuration to use to start the broker.
     * @param handlers the handlers to install in the broker.
     * @throws IOException in case of any IO Error.
     */
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
        initInterceptors(config, handlers);
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
        SessionRegistry sessions = new SessionRegistry(subscriptions, interceptor);
        dispatcher = new PostOffice(subscriptions, authorizatorPolicy, new MemoryRetainedRepository(), sessions,
                                    interceptor);
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

    private void initInterceptors(IConfig props, List<? extends InterceptHandler> embeddedObservers) {
        LOG.info("Configuring message interceptors...");

        List<InterceptHandler> observers = new ArrayList<>(embeddedObservers);
        String interceptorClassName = props.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (interceptorClassName != null && !interceptorClassName.isEmpty()) {
            InterceptHandler handler = loadClass(interceptorClassName, InterceptHandler.class, io.moquette.broker.Server.class, this);
            if (handler != null) {
                observers.add(handler);
            }
        }
        interceptor = new BrokerInterceptor(props, observers);
    }

    @SuppressWarnings("unchecked")
    private <T, U> T loadClass(String className, Class<T> intrface, Class<U> constructorArgClass, U props) {
        T instance = null;
        try {
            // check if constructor with constructor arg class parameter
            // exists
            LOG.info("Invoking constructor with {} argument. ClassName={}, interfaceName={}",
                     constructorArgClass.getName(), className, intrface.getName());
            instance = this.getClass().getClassLoader()
                .loadClass(className)
                .asSubclass(intrface)
                .getConstructor(constructorArgClass)
                .newInstance(props);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            LOG.warn("Unable to invoke constructor with {} argument. ClassName={}, interfaceName={}, cause={}, " +
                     "errorMessage={}", constructorArgClass.getName(), className, intrface.getName(), ex.getCause(),
                     ex.getMessage());
            return null;
        } catch (NoSuchMethodException | InvocationTargetException e) {
            try {
                LOG.info("Invoking default constructor. ClassName={}, interfaceName={}", className, intrface.getName());
                // fallback to default constructor
                instance = this.getClass().getClassLoader()
                    .loadClass(className)
                    .asSubclass(intrface)
                    .getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
                NoSuchMethodException | InvocationTargetException ex) {
                LOG.error("Unable to invoke default constructor. ClassName={}, interfaceName={}, cause={}, " +
                          "errorMessage={}", className, intrface.getName(), ex.getCause(), ex.getMessage());
                return null;
            }
        }

        return instance;
    }

    /**
     * Use the broker to publish a message. It's intended for embedding applications. It can be used
     * only after the server is correctly started with startServer.
     *
     * @param msg      the message to forward.
     * @param clientId the id of the sending server.
     * @throws IllegalStateException if the server is not yet started
     */
    public void internalPublish(MqttPublishMessage msg, final String clientId) {
        final int messageID = msg.variableHeader().messageId();
        if (!initialized) {
            LOG.error("Moquette is not started, internal message cannot be published. CId={}, messageId={}", clientId,
                messageID);
            throw new IllegalStateException("Can't publish on a server is not yet started");
        }
        LOG.debug("Publishing message. CId={}, messageId={}", clientId, messageID);
        dispatcher.internalPublish(msg);
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

    /**
     * SPI method used by Broker embedded applications to get list of subscribers. Returns null if
     * the broker is not started.
     *
     * @return list of subscriptions.
     */
// TODO reimplement this
//    public List<Subscription> getSubscriptions() {
//        if (m_processorBootstrapper == null) {
//            return null;
//        }
//        return this.subscriptionsStore.listAllSubscriptions();
//    }

    /**
     * SPI method used by Broker embedded applications to add intercept handlers.
     *
     * @param interceptHandler the handler to add.
     */
    public void addInterceptHandler(InterceptHandler interceptHandler) {
        if (!initialized) {
            LOG.error("Moquette is not started, MQTT message interceptor cannot be added. InterceptorId={}",
                interceptHandler.getID());
            throw new IllegalStateException("Can't register interceptors on a server that is not yet started");
        }
        LOG.info("Adding MQTT message interceptor. InterceptorId={}", interceptHandler.getID());
        interceptor.addInterceptHandler(interceptHandler);
    }

    /**
     * SPI method used by Broker embedded applications to remove intercept handlers.
     *
     * @param interceptHandler the handler to remove.
     */
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        if (!initialized) {
            LOG.error("Moquette is not started, MQTT message interceptor cannot be removed. InterceptorId={}",
                interceptHandler.getID());
            throw new IllegalStateException("Can't deregister interceptors from a server that is not yet started");
        }
        LOG.info("Removing MQTT message interceptor. InterceptorId={}", interceptHandler.getID());
        interceptor.removeInterceptHandler(interceptHandler);
    }
}
