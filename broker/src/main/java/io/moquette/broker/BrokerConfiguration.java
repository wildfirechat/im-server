package io.moquette.broker;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;

class BrokerConfiguration {

    private final boolean allowAnonymous;
    private final boolean allowZeroByteClientId;
    private final boolean reauthorizeSubscriptionsOnConnect;

    BrokerConfiguration(IConfig props) {
        allowAnonymous = props.boolProp(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, true);
        allowZeroByteClientId = props.boolProp(BrokerConstants.ALLOW_ZERO_BYTE_CLIENT_ID_PROPERTY_NAME, false);
        reauthorizeSubscriptionsOnConnect = props.boolProp(BrokerConstants.REAUTHORIZE_SUBSCRIPTIONS_ON_CONNECT, false);
    }

    public BrokerConfiguration(boolean allowAnonymous, boolean allowZeroByteClientId,
                               boolean reauthorizeSubscriptionsOnConnect) {
        this.allowAnonymous = allowAnonymous;
        this.allowZeroByteClientId = allowZeroByteClientId;
        this.reauthorizeSubscriptionsOnConnect = reauthorizeSubscriptionsOnConnect;
    }

    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    public boolean isAllowZeroByteClientId() {
        return allowZeroByteClientId;
    }

    public boolean isReauthorizeSubscriptionsOnConnect() {
        return reauthorizeSubscriptionsOnConnect;
    }
}
