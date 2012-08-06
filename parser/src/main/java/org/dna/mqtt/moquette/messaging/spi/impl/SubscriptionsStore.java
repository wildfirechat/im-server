package org.dna.mqtt.moquette.messaging.spi.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tree of published topics
 *
 * @author andrea
 */
public class SubscriptionsStore {
    
    protected static class Token {
    
        static final Token EMPTY = new Token("");
        static final Token MULTI = new Token("#");
        static final Token SINGLE = new Token("+");
        
        String name;
        
        protected Token(String s) {
            name = s;
        }
        
        protected String name() {
            return name;
        }
        
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
    
    protected List<Token> splitTopic(String topic) throws ParseException {
        List res = new ArrayList<Token>();
        String[] splitted = topic.split("/");
        
        for (int i=0; i<splitted.length; i++) {
            String s = splitted[i];
            if (s.isEmpty()) {
                if (i != 0){
                    throw new ParseException("Bad format of topic, expetec topic name between separators", i);
                }
                res.add(Token.EMPTY);
            } else if (s.equals("#")) {
                //check that multi is the last symbol
                if (i != splitted.length - 1 ) {
                    throw new ParseException("Bad format of topic, the multi symbol (#) has to be the last one after a separator", i);
                }
                res.add(Token.MULTI);
            } else if (s.contains("#")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else if (s.equals("+")) {
                res.add(Token.SINGLE);
            } else if (s.contains("+")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else {
                res.add(new Token(s));
            }
        }
        
        return res;
    }
}
