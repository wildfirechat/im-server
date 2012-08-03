package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tree of published topics
 *
 * @author andrea
 */
public class SubscriptionsStore {
    
    protected static class Token {

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Token other = (Token) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return true;
        }
        String name;
        protected Token(String s) {
            name = s;
        }
        
        protected String name() {
            return name;
        }
    }

    private List<Subscription> subscriptions = new ArrayList<Subscription>();

    public void add(Subscription newSubscription) {
        if (subscriptions.contains(newSubscription)) {
            return;
        }
        subscriptions.add(newSubscription);
    }

    public List<Subscription> matches(String topic) {
        List<Subscription> m = new ArrayList<Subscription>();
        for (Subscription sub : subscriptions) {
            if (sub.match(topic)) {
                m.add(sub);
            }
        }
        return m;
    }

    public boolean contains(Subscription sub) {
        return subscriptions.contains(sub);
    }

    public int size() {
        return subscriptions.size();
    }
    
    protected List<Token> splitTopic(String topic) {
        List res = new ArrayList<Token>();
        for (String s : topic.split("/")) {
            res.add(new Token(s));
        }
        
        return res;
    }
}
