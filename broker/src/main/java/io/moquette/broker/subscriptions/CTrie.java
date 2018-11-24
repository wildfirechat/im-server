package io.moquette.broker.subscriptions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CTrie {

    interface IVisitor<T> {

        void visit(CNode node, int deep);

        T getResult();
    }

    private static final Token ROOT = new Token("root");
    private static final INode NO_PARENT = null;

    private enum Action {
        OK, REPEAT
    }

    INode root;

    CTrie() {
        final CNode mainNode = new CNode();
        mainNode.token = ROOT;
        this.root = new INode(mainNode);
    }

    Optional<CNode> lookup(Topic topic) {
        INode inode = this.root;
        Token token = topic.headToken();
        while (!topic.isEmpty() && (inode.mainNode().anyChildrenMatch(token))) {
            topic = topic.exceptHeadToken();
            inode = inode.mainNode().childOf(token);
            token = topic.headToken();
        }
        if (inode == null || !topic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(inode.mainNode());
    }

    enum NavigationAction {
        MATCH, GODEEP, STOP
    }

    private NavigationAction evaluate(Topic topic, CNode cnode) {
        if (Token.MULTI.equals(cnode.token)) {
            return NavigationAction.MATCH;
        }
        if (topic.isEmpty()) {
            return NavigationAction.STOP;
        }
        final Token token = topic.headToken();
        if (!(Token.SINGLE.equals(cnode.token) || cnode.token.equals(token) || ROOT.equals(cnode.token))) {
            return NavigationAction.STOP;
        }
        return NavigationAction.GODEEP;
    }

    public Set<Subscription> recursiveMatch(Topic topic) {
        return recursiveMatch(topic, this.root);
    }

    private Set<Subscription> recursiveMatch(Topic topic, INode inode) {
        CNode cnode = inode.mainNode();
        NavigationAction action = evaluate(topic, cnode);
        if (action == NavigationAction.MATCH) {
            return cnode.subscriptions;
        }
        if (action == NavigationAction.STOP) {
            return Collections.emptySet();
        }
        if (cnode instanceof TNode) {
            return Collections.emptySet();
        }
        Topic remainingTopic = (ROOT.equals(cnode.token)) ? topic : topic.exceptHeadToken();
        Set<Subscription> subscriptions = new HashSet<>();
        if (remainingTopic.isEmpty()) {
            subscriptions.addAll(cnode.subscriptions);
        }
        for (INode subInode : cnode.allChildren()) {
            subscriptions.addAll(recursiveMatch(remainingTopic, subInode));
        }
        return subscriptions;
    }

    public void addToTree(Subscription newSubscription) {
        Action res;
        do {
            res = insert(newSubscription.topicFilter, this.root, newSubscription);
        } while (res == Action.REPEAT);
    }

    private Action insert(Topic topic, final INode inode, Subscription newSubscription) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && inode.mainNode().anyChildrenMatch(token)) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return insert(remainingTopic, nextInode, newSubscription);
        } else {
            if (topic.isEmpty()) {
                return insertSubscription(inode, newSubscription);
            } else {
                return createNodeAndInsertSubscription(topic, inode, newSubscription);
            }
        }
    }

    private Action insertSubscription(INode inode, Subscription newSubscription) {
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy().addSubscription(newSubscription);
        if (inode.compareAndSet(cnode, updatedCnode)) {
            return Action.OK;
        } else {
            return Action.REPEAT;
        }
    }

    private Action createNodeAndInsertSubscription(Topic topic, INode inode, Subscription newSubscription) {
        INode newInode = createPathRec(topic, newSubscription);
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy();
        updatedCnode.add(newInode);

        return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
    }

    private INode createPathRec(Topic topic, Subscription newSubscription) {
        Topic remainingTopic = topic.exceptHeadToken();
        if (!remainingTopic.isEmpty()) {
            INode inode = createPathRec(remainingTopic, newSubscription);
            CNode cnode = new CNode();
            cnode.token = topic.headToken();
            cnode.add(inode);
            return new INode(cnode);
        } else {
            return createLeafNodes(topic.headToken(), newSubscription);
        }
    }

    private INode createLeafNodes(Token token, Subscription newSubscription) {
        CNode newLeafCnode = new CNode();
        newLeafCnode.token = token;
        newLeafCnode.addSubscription(newSubscription);

        return new INode(newLeafCnode);
    }

    public void removeFromTree(Topic topic, String clientID) {
        Action res;
        do {
            res = remove(clientID, topic, this.root, NO_PARENT);
        } while (res == Action.REPEAT);
    }

    private Action remove(String clientId, Topic topic, INode inode, INode iParent) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && (inode.mainNode().anyChildrenMatch(token))) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return remove(clientId, remainingTopic, nextInode, inode);
        } else {
            final CNode cnode = inode.mainNode();
            if (cnode instanceof TNode) {
                // this inode is a tomb, has no clients and should be cleaned up
                // Because we implemented cleanTomb below, this should be rare, but possible
                // Consider calling cleanTomb here too
                return Action.OK;
            }
            if (cnode.containsOnly(clientId) && topic.isEmpty() && cnode.allChildren().isEmpty()) {
                // last client to leave this node, AND there are no downstream children, remove via TNode tomb
                if (inode == this.root) {
                    return inode.compareAndSet(cnode, inode.mainNode().copy()) ? Action.OK : Action.REPEAT;
                }
                TNode tnode = new TNode();
                return inode.compareAndSet(cnode, tnode) ? cleanTomb(inode, iParent) : Action.REPEAT;
            } else if (cnode.contains(clientId) && topic.isEmpty()) {
                CNode updatedCnode = cnode.copy();
                updatedCnode.removeSubscriptionsFor(clientId);
                return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
            } else {
                //someone else already removed
                return Action.OK;
            }
        }
    }

    /**
     *
     * Cleans Disposes of TNode in separate Atomic CAS operation per
     * http://bravenewgeek.com/breaking-and-entering-lose-the-lock-while-embracing-concurrency/
     *
     * We roughly follow this theory above, but we allow CNode with no Subscriptions to linger (for now).
     *
     *
     * @param inode inode that handle to the tomb node.
     * @param iParent inode parent.
     * @return REPEAT if the this methods wasn't successful or OK.
     */
    private Action cleanTomb(INode inode, INode iParent) {
        CNode updatedCnode = iParent.mainNode().copy();
        updatedCnode.remove(inode);
        return iParent.compareAndSet(iParent.mainNode(), updatedCnode) ? Action.OK : Action.REPEAT;
    }

    public int size() {
        SubscriptionCounterVisitor visitor = new SubscriptionCounterVisitor();
        dfsVisit(this.root, visitor, 0);
        return visitor.getResult();
    }

    public String dumpTree() {
        DumpTreeVisitor visitor = new DumpTreeVisitor();
        dfsVisit(this.root, visitor, 0);
        return visitor.getResult();
    }

    private void dfsVisit(INode node, IVisitor<?> visitor, int deep) {
        if (node == null) {
            return;
        }

        visitor.visit(node.mainNode(), deep);
        ++deep;
        for (INode child : node.mainNode().allChildren()) {
            dfsVisit(child, visitor, deep);
        }
    }
}
