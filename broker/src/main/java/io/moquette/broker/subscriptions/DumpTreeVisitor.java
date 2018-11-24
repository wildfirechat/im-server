/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.broker.subscriptions;

import io.netty.util.internal.StringUtil;

class DumpTreeVisitor implements CTrie.IVisitor<String> {

    String s = "";

    @Override
    public void visit(CNode node, int deep) {
        String indentTabs = indentTabs(deep);
        s += indentTabs + (node.token == null ? "''" : node.token.toString()) + prettySubscriptions(node) + "\n";
    }

    private String prettySubscriptions(CNode node) {
        if (node instanceof TNode) {
            return "TNode";
        }
        if (node.subscriptions.isEmpty()) {
            return StringUtil.EMPTY_STRING;
        }
        StringBuilder subScriptionsStr = new StringBuilder(" ~~[");
        int counter = 0;
        for (Subscription couple : node.subscriptions) {
            subScriptionsStr
                .append("{filter=").append(couple.topicFilter).append(", ")
                .append("qos=").append(couple.getRequestedQos()).append(", ")
                .append("client='").append(couple.clientId).append("'}");
            counter++;
            if (counter < node.subscriptions.size()) {
                subScriptionsStr.append(";");
            }
        }
        return subScriptionsStr.append("]").toString();
    }

    private String indentTabs(int deep) {
        StringBuilder s = new StringBuilder();
        if (deep > 0) {
            s.append("    ");
            for (int i = 0; i < deep - 1; i++) {
                s.append("| ");
            }
            s.append("|-");
        }
        return s.toString();
    }

    @Override
    public String getResult() {
        return s;
    }
}
