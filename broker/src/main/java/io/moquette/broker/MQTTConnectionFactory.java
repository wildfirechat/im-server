package io.moquette.broker;

import io.moquette.spi.security.IAuthenticator;
import io.netty.channel.Channel;

class MQTTConnectionFactory {

    private final BrokerConfiguration brokerConfig;
    private final IAuthenticator authenticator;
    private final SessionRegistry sessionRegistry;
    private final PostOffice postOffice;

    MQTTConnectionFactory(BrokerConfiguration brokerConfig, IAuthenticator authenticator,
                          SessionRegistry sessionRegistry, PostOffice postOffice) {
        this.brokerConfig = brokerConfig;
        this.authenticator = authenticator;
        this.sessionRegistry = sessionRegistry;
        this.postOffice = postOffice;
    }

    public MQTTConnection create(Channel channel) {
        return new MQTTConnection(channel, brokerConfig, authenticator, sessionRegistry, postOffice);
    }
}
