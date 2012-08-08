package org.dna.mqtt.moquette.messaging.spi.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a tree of published topics
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
            if (t == null) {
                matchingSubs.addAll(m_subscriptions);
                return;
            }
            
            //check the matching of the current token
            //TODO take care of MULTI and SINGLE
//            if (m_token == Token.MULTI) {
//                matchingSubs.addAll(m_subscriptions);
//                return;
//            }
            
            List<TreeNode> matchingChildren = new ArrayList<TreeNode>();
            for (TreeNode child : m_children) {
                if (child.getToken().match(t)) {
                    matchingChildren.add(child);
                }
            }
            
//            if (matchingChildren.isEmpty()) {
//                return;
//            }
            
            for (TreeNode child : matchingChildren) {
                if (child.getToken() == Token.MULTI) {
                    matchingSubs.addAll(m_subscriptions);
                } else {
                    child.matches(tokens, matchingSubs);
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
        
        /**
         * Return true iff the current token is compatible with t.
         * 
         * TODO define the rules of compatibility
         * if this is a MULTI or SINGLE and the other is any string => match
         * of this is a normal token and also the other, and they are equals => match
         * if the other is not normal token => don't match
         */
        protected boolean match(Token t) {
            if (t == MULTI || t == SINGLE) {
                return false;
            }
            
            if (this == MULTI || this == SINGLE) {
                return true;
            } else {
                return equals(t);
            }
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

//    private List<Subscription> subscriptions = new ArrayList<Subscription>();
    private TreeNode subscriptionsRoot = new TreeNode(null);

    public void add(Subscription newSubscription) {
        List<Token> tokens = new ArrayList<Token>();
        try {
            tokens = splitTopic(newSubscription.topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            Logger.getLogger(SubscriptionsStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Deep into the tree creating missing nodes where necessary
        TreeNode current = subscriptionsRoot;
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
        current.addSubcription(newSubscription);
    }

    public List<Subscription> matches(String topic) {
        List<Token> tokens = new ArrayList<Token>();
        try {
            tokens = splitTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            Logger.getLogger(SubscriptionsStore.class.getName()).log(Level.SEVERE, null, ex);
        }
//        TreeNode current = subscriptionsRoot;
        List<Subscription> matchingSubscriptions = new ArrayList<Subscription>();
        subscriptionsRoot.matches(new LinkedBlockingQueue<Token>(tokens), matchingSubscriptions);
        return matchingSubscriptions;
//        for (Token token : tokens) {
//            current = current.matchNext(token);
//            if (current == null) {
//                break;
//            }
//        }
//        if (current != null) {
//            return current.subscriptions();
//        } else {
//            return Collections.EMPTY_LIST;
//        }
    }

    public boolean contains(Subscription sub) {
        return !matches(sub.topic).isEmpty();
    }
    
    public int size() {
        return subscriptionsRoot.size();
    }
    
    protected List<Token> splitTopic(String topic) throws ParseException {
        List res = new ArrayList<Token>();
        String[] splitted = topic.split("/");
        
        if (splitted.length == 0) {
            res.add(Token.EMPTY);
        }
        
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
