package io.moquette.spi;

import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;

import java.util.List;

public interface ISubscriptionsStore {
    class ClientTopicCouple {

        public final Topic topicFilter;
        public final String clientID;

        public ClientTopicCouple(String clientID, Topic topicFilter) {
            this.clientID = clientID;
            this.topicFilter = topicFilter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ClientTopicCouple that = (ClientTopicCouple) o;

            if (topicFilter != null ? !topicFilter.equals(that.topicFilter) : that.topicFilter != null)
                return false;
            return !(clientID != null ? !clientID.equals(that.clientID) : that.clientID != null);
        }

        @Override
        public int hashCode() {
            int result = topicFilter != null ? topicFilter.hashCode() : 0;
            result = 31 * result + (clientID != null ? clientID.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ClientTopicCouple{" + "topicFilter='" + topicFilter + '\'' + ", clientID='" + clientID + '\'' + '}';
        }
    }


    /**
     * Add a new subscription to the session
     *
     * @param newSubscription
     *            the subscription to add.
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     *
     * @param topic
     *            the topic of the subscription.
     * @param clientID
     *            the session client.
     */
    void removeSubscription(Topic topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     *
     * @param sessionID
     *            the client ID
     */
    void wipeSubscriptions(String sessionID);

    /**
     * Return all topic filters to recreate the subscription tree.
     */
    List<ClientTopicCouple> listAllSubscriptions();

    /**
     * @param couple
     *            the subscription descriptor.
     * @return the subscription stored by clientID and topicFilter, if any else null;
     */
    Subscription getSubscription(ClientTopicCouple couple);

    /*
     * @return all subscriptions stored.
     */
    List<Subscription> getSubscriptions();

}
