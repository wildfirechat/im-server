package org.dna.mqtt.moquette.messaging.spi.impl.subscriptions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a tree of topics subscriptions.
 *
 * @author andrea
 */
public class SubscriptionsStore {

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

    public void deactivate(String clientID) {
        subscriptions.deactivate(clientID);
    }

    public void activate(String clientID) {
        LOG.debug("activate re-activating subscriptions for clientID " + clientID);
        subscriptions.activate(clientID);
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

    /**
     * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
     */
    public static boolean matchTopics(String msgTopic, String subscriptionTopic) {
        try {
            List<Token> msgTokens = SubscriptionsStore.splitTopic(msgTopic);
            List<Token> subscriptionTokens = SubscriptionsStore.splitTopic(subscriptionTopic);
            int i = 0;
            for (; i< subscriptionTokens.size(); i++) {
                Token subToken = subscriptionTokens.get(i);
                if (subToken != Token.MULTI && subToken != Token.SINGLE) {
                    Token msgToken = msgTokens.get(i);
                    if (!msgToken.equals(subToken)) {
                        return false;
                    }
                } else {
                    if (subToken == Token.MULTI) {
                        return true;
                    }
                    if (subToken == Token.SINGLE) {
                        //skip a step forward
                    }
                }
            }
            return i == msgTokens.size();
        } catch (ParseException ex) {
            LOG.error(null, ex);
            throw new RuntimeException(ex);
        }
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
