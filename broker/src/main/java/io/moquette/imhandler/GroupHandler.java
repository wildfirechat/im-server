/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import win.liyufan.im.MessageShardingUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract public class GroupHandler<T> extends IMHandler<T> {
    protected void sendGroupNotification(String fromUser, String targetId, List<Integer> lines, WFCMessage.MessageContent content) {
        sendGroupNotification(fromUser, targetId, lines, content, null);
    }

    protected void sendGroupNotification(String fromUser, String targetId, List<Integer> lines, WFCMessage.MessageContent content, Collection<String> toUsers) {
        if (lines == null) {
            lines = new ArrayList<>();
        } else {
            lines = new ArrayList<>(lines);
        }

        if (lines.isEmpty()) {
            lines.add(0);
        }

        for (int line : lines) {
            long timestamp = System.currentTimeMillis();
            WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder().setContent(content).setServerTimestamp(timestamp);
            builder.setConversation(builder.getConversationBuilder().setType(ProtoConstants.ConversationType.ConversationType_Group).setTarget(targetId).setLine(line));
            builder.setFromUser(fromUser);
            if(toUsers != null && !toUsers.isEmpty()) {
                builder.addAllTo(toUsers);
            }
            try {
                long messageId = MessageShardingUtil.generateId();
                builder.setMessageId(messageId);
                saveAndPublish(fromUser, null, builder.build(), ProtoConstants.RequestSourceType.Request_From_User);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected List<String> getMemberIdList(List<WFCMessage.GroupMember> groupMembers) {
        List<String> out = new ArrayList<>();
        if (groupMembers != null) {
            for (WFCMessage.GroupMember gm : groupMembers
                 ) {
                out.add(gm.getMemberId());
            }
        }
        return out;
    }
}
