package io.moquette.spi.impl;

import io.moquette.connections.IConnectionsManager;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

public class DisconnectHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DisconnectHandler.class);

    private final IConnectionsManager connectionDescriptors;
    private final SessionsRepository sessionsRepository;
    private final ISubscriptionsDirectory subscriptions;
    private final BrokerInterceptor m_interceptor;

    public DisconnectHandler(IConnectionsManager connectionDescriptors, SessionsRepository sessionsRepository,
                             ISubscriptionsDirectory subscriptions, BrokerInterceptor interceptor) {
        this.connectionDescriptors = connectionDescriptors;
        this.sessionsRepository = sessionsRepository;
        this.subscriptions = subscriptions;
        this.m_interceptor = interceptor;
    }

    public void processDisconnect(Channel channel, String clientID) {
        final Optional<ConnectionDescriptor> existingDescriptorOpt = this.connectionDescriptors.lookupDescriptor(clientID);
        if (!existingDescriptorOpt.isPresent()) {
            // another client with same ID removed the descriptor, we must exit
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }
        final ConnectionDescriptor existingDescriptor = existingDescriptorOpt.get();

        if (existingDescriptor.doesNotUseChannel(channel)) {
            // another client saved it's descriptor, exit
            LOG.warn("Another client is using the connection descriptor. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!removeSubscriptions(existingDescriptor, clientID)) {
            LOG.warn("Unable to remove subscriptions. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!dropStoredMessages(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop stored messages. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!cleanWillMessageAndNotifyInterceptor(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop will message. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!existingDescriptor.close()) {
            LOG.debug("Connection has been closed. CId={}", clientID);
            return;
        }

        boolean stillPresent = this.connectionDescriptors.removeConnection(existingDescriptor);
        if (!stillPresent) {
            // another descriptor was inserted
            LOG.warn("Another descriptor has been inserted. CId={}", clientID);
            return;
        }
        this.sessionsRepository.disconnect(clientID);

        LOG.info("Client <{}> disconnected", clientID);
    }

    private boolean removeSubscriptions(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ESTABLISHED, SUBSCRIPTIONS_REMOVED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.trace("Removing saved subscriptions. CId={}", descriptor.clientID);
            final ClientSession session = this.sessionsRepository.sessionForClient(clientID);
            session.wipeSubscriptions();
            for (Subscription existingSub : session.getSubscriptions()) {
                this.subscriptions.removeSubscription(existingSub.getTopicFilter(), clientID);
            }
            LOG.trace("Saved subscriptions have been removed. CId={}", descriptor.clientID);
        }
        return true;
    }

    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(SUBSCRIPTIONS_REMOVED, MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        final ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        if (clientSession.isCleanSession()) {
            clientSession.dropQueue();
        }
        return true;
    }

    private boolean cleanWillMessageAndNotifyInterceptor(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(MESSAGES_DROPPED, INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        LOG.trace("Removing will message. ClientId={}", descriptor.clientID);
        // cleanup the will store
        final ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        clientSession.removeWill();

        String username = descriptor.getUsername();
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }


}
