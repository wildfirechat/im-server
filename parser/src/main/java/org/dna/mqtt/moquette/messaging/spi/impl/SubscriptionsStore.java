package org.dna.mqtt.moquette.messaging.spi.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.fusesource.hawtbuf.codec.StringCodec;
import org.fusesource.hawtdb.api.BTreeIndexFactory;
import org.fusesource.hawtdb.api.MultiIndexFactory;
import org.fusesource.hawtdb.api.SortedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a tree of topics subscriptions.
 *
 * @author andrea
 */
public class SubscriptionsStore {

    private class TreeNode {

        TreeNode m_parent;
        Token m_token;
        List<TreeNode> m_children = new ArrayList<TreeNode>();
        List<Subscription> m_subscriptions = new ArrayList<Subscription>();

        TreeNode(TreeNode parent) {
            this.m_parent = parent;
        }

        Token getToken() {
            return m_token;
        }

        void setToken(Token topic) {
            this.m_token = topic;
        }

        void addSubcription(Subscription s) {
            //avoid double registering
            if (m_subscriptions.contains(s)) {
                return;
            }
            m_subscriptions.add(s);
        }

        void addChild(TreeNode child) {
            m_children.add(child);
        }

        boolean isLeaf() {
            return m_children.isEmpty();
        }

        /**
         * Search for children that has the specified token, if not found return
         * null;
         */
        TreeNode childWithToken(Token token) {
            for (TreeNode child : m_children) {
                if (child.getToken().equals(token)) {
                    return child;
                }
            }

            return null;
        }

        List<Subscription> subscriptions() {
            return m_subscriptions;
        }

        void matches(Queue<Token> tokens, List<Subscription> matchingSubs) {
            Token t = tokens.poll();

            //check if t is null <=> tokens finished
            if (t == null) {
                matchingSubs.addAll(m_subscriptions);
                //check if it has got a MULTI child and add its subscriptions
                for (TreeNode n : m_children) {
                    if (n.getToken() == Token.MULTI) {
                        matchingSubs.addAll(n.subscriptions());
                    }
                }

                return;
            }

            //we are on MULTI, than add subscriptions and return
            if (m_token == Token.MULTI) {
                matchingSubs.addAll(m_subscriptions);
                return;
            }

            for (TreeNode n : m_children) {
                if (n.getToken().match(t)) {
                    //Create a copy of token, alse if navigate 2 sibling it 
                    //consumes 2 elements on the queue instead of one
                    n.matches(new LinkedBlockingQueue<Token>(tokens), matchingSubs);
                }
            }
        }

        /**
         * Return the number of registered subscriptions
         */
        int size() {
            int res = m_subscriptions.size();
            for (TreeNode child : m_children) {
                res += child.size();
            }
            return res;
        }

        void removeClientSubscriptions(String clientID) {
            //collect what to delete and then delete to avoid ConcurrentModification
            List<Subscription> subsToRemove = new ArrayList<Subscription>();
            for (Subscription s : m_subscriptions) {
                if (s.clientId.equals(clientID)) {
                    subsToRemove.add(s);
                }
            }

            for (Subscription s : subsToRemove) {
                m_subscriptions.remove(s);
            }

            //go deep
            for (TreeNode child : m_children) {
                child.removeClientSubscriptions(clientID);
            }
        }

        /**
         * Deactivate all topic subscriptions for the given clientID.
         * */
        void disconnect(String clientID) {
            for (Subscription s : m_subscriptions) {
                if (s.clientId.equals(clientID)) {
                    s.setActive(false);
                }
            }

            //go deep
            for (TreeNode child : m_children) {
                child.disconnect(clientID);
            }
        }

        /**
         * Activate all topic subscriptions for the given clientID.
         * */
        public void connect(String clientID) {
            for (Subscription s : m_subscriptions) {
                if (s.clientId.equals(clientID)) {
                    s.setActive(true);
                }
            }

            //go deep
            for (TreeNode child : m_children) {
                child.disconnect(clientID);
            }

        }
    }

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

        protected boolean match(Token t) {
            if (t == MULTI || t == SINGLE) {
                return false;
            }

            if (this == MULTI || this == SINGLE) {
                return true;
            }

            return equals(t);
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

        @Override
        public String toString() {
            return name;
        }
    }
    private TreeNode subscriptions = new TreeNode(null);
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsStore.class);

    private IStorageService m_storageService;

    /**
     * Initialize basic store structures, like the FS storage to maintain
     * client's topics subscriptions
     */
    public void init(IStorageService storageService) {
        LOG.debug("init invoked");

        m_storageService = storageService;

        //reload any subscriptions persisted
        LOG.debug("Reloading all stored subscriptions...");
        for (Subscription subscription : m_storageService.retrieveAllSubscriptions()) {
            LOG.debug("Re-subscribing " + subscription.getClientId() + " to topic " + subscription.getTopic());
            addDirect(subscription);
        }
        LOG.debug("Finished loading");
    }
    
    protected void addDirect(Subscription newSubscription) {
        TreeNode current = findMatchingNode(newSubscription.topic);
        current.addSubcription(newSubscription);
    }
    
    private TreeNode findMatchingNode(String topic) {
        List<Token> tokens = new ArrayList<Token>();
        try {
            tokens = splitTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            LOG.error(null, ex);
//            return;
        }

        TreeNode current = subscriptions;
        for (Token token : tokens) {
            TreeNode matchingChildren;

            //check if a children with the same token already exists
            if ((matchingChildren = current.childWithToken(token)) != null) {
                current = matchingChildren;
            } else {
                //create a new node for the newly inserted token
                matchingChildren = new TreeNode(current);
                matchingChildren.setToken(token);
                current.addChild(matchingChildren);
                current = matchingChildren;
            }
        }
        return current;
    }

    public void add(Subscription newSubscription) {
        addDirect(newSubscription);

        //log the subscription
        String clientID = newSubscription.getClientId();
        m_storageService.addNewSubscription(newSubscription, clientID);
//        pageFile.flush();
    }


    public void removeSubscription(String topic, String clientID) {
        TreeNode matchNode = findMatchingNode(topic);
        
        //search fr the subscription to remove
        Subscription toBeRemoved = null;
        for (Subscription sub : matchNode.subscriptions()) {
            if (sub.topic.equals(topic)) {
                toBeRemoved = sub;
                break;
            }
        }
        
        if (toBeRemoved != null) {
            matchNode.subscriptions().remove(toBeRemoved);
        }
    }

    /**
     * Visit the topics tree to remove matching subscriptions with clientID
     */
    public void removeForClient(String clientID) {
        subscriptions.removeClientSubscriptions(clientID);

        //remove from log all subscriptions
        m_storageService.removeAllSubscriptions(clientID);
    }

    public void disconnect(String clientID) {
        subscriptions.disconnect(clientID);
    }

    public void connect(String clientID) {
        LOG.debug("connect re-activating subscriptions for clientID " + clientID);
        subscriptions.connect(clientID);
    }

    public List<Subscription> matches(String topic) {
        List<Token> tokens = new ArrayList<Token>();
        try {
            tokens = splitTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            LOG.error(null, ex);
        }

        Queue<Token> tokenQueue = new LinkedBlockingDeque<Token>(tokens);
        List<Subscription> matchingSubs = new ArrayList<Subscription>();
        subscriptions.matches(tokenQueue, matchingSubs);
        return matchingSubs;
    }

    public boolean contains(Subscription sub) {
        return !matches(sub.topic).isEmpty();
    }

    public int size() {
        return subscriptions.size();
    }

    
    protected static List<Token> splitTopic(String topic) throws ParseException {
        List res = new ArrayList<Token>();
        String[] splitted = topic.split("/");

        if (splitted.length == 0) {
            res.add(Token.EMPTY);
        }

        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            if (s.isEmpty()) {
                if (i != 0) {
                    throw new ParseException("Bad format of topic, expetec topic name between separators", i);
                }
                res.add(Token.EMPTY);
            } else if (s.equals("#")) {
                //check that multi is the last symbol
                if (i != splitted.length - 1) {
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
