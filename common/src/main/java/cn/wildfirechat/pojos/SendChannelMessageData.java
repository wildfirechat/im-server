/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class SendChannelMessageData {
    private List<String> targets;
    private int line;
    private MessagePayload payload;

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    public void setPayload(MessagePayload payload) {
        this.payload = payload;
    }

    public static boolean isValide(SendChannelMessageData sendMessageData) {
        if(sendMessageData == null ||
            sendMessageData.getPayload() == null) {
            return false;
        }
        return true;
    }

    public WFCMessage.Message toProtoMessage(String channelId, String channelOwner) {
        if (targets == null) {
            targets = new ArrayList<>();
        }

        return WFCMessage.Message.newBuilder().setFromUser(channelOwner)
            .setConversation(WFCMessage.Conversation.newBuilder().setType(ProtoConstants.ConversationType.ConversationType_Channel).setTarget(channelId).setLine(line))
            .addAllTo(targets)
            .setContent(payload.toProtoMessageContent())
            .build();
    }
}
