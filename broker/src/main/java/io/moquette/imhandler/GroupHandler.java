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
import java.util.List;

abstract public class GroupHandler<T> extends IMHandler<T> {
    protected void sendGroupNotification(String fromUser, String targetId, List<Integer> lines, WFCMessage.MessageContent content) {
        if (lines != null) {
            lines = new ArrayList<>();
        }

        if (lines.isEmpty()) {
            lines.add(0);
        }

        for (int line : lines) {
            long timestamp = System.currentTimeMillis();
            WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder().setContent(content).setServerTimestamp(timestamp);
            builder.setConversation(builder.getConversationBuilder().setType(ProtoConstants.ConversationType.ConversationType_Group).setTarget(targetId).setLine(line));
            builder.setFromUser(fromUser);
            long messageId = MessageShardingUtil.generateId();
            builder.setMessageId(messageId);
            saveAndPublish(fromUser, null, builder.build());
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
