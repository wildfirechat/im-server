/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;


import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.SendChannelMessageData;
import cn.wildfirechat.proto.ProtoConstants;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.Conversation;
import cn.wildfirechat.pojos.SendMessageData;
import cn.wildfirechat.pojos.SendMessageResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import static cn.wildfirechat.proto.ProtoConstants.ConversationType.ConversationType_Channel;

@Route(APIPath.Channel_Message_Send)
@HttpMethod("POST")
public class SendMessageAction extends ChannelAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            SendChannelMessageData sendChannelMessageData = getRequestBody(request.getNettyRequest(), SendChannelMessageData.class);
            if(sendChannelMessageData.getTargets() != null && !sendChannelMessageData.getTargets().isEmpty()) {
                if((channelInfo.getStatus() & ProtoConstants.ChannelState.Channel_State_Mask_Message_Unsubscribed) == 0 && (channelInfo.getStatus() & ProtoConstants.ChannelState.Channel_State_Mask_Global) == 0) {
                    for (String target:sendChannelMessageData.getTargets()) {
                        if(!messagesStore.checkUserInChannel(target, channelInfo.getTargetId())) {
                            setResponseContent(RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_RIGHT, "User " + target + " not in channel"), response);
                            return true;
                        }
                    }
                }
            }

            SendMessageData sendMessageData = new SendMessageData();
            sendMessageData.setConv(new Conversation());
            sendMessageData.getConv().setType(ConversationType_Channel);
            sendMessageData.getConv().setTarget(channelInfo.getTargetId());
            sendMessageData.getConv().setLine(sendChannelMessageData.getLine());
            sendMessageData.setSender(channelInfo.getOwner());
            sendMessageData.setPayload(sendChannelMessageData.getPayload());
            sendMessageData.setToUsers(sendChannelMessageData.getTargets());
            if (SendMessageData.isValide(sendMessageData)) {
                sendApiMessage(response, IMTopic.SendMessageTopic, sendMessageData.toProtoMessage().toByteArray(), result -> {
                    ByteBuf byteBuf = Unpooled.buffer();
                    byteBuf.writeBytes(result);
                    ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                    if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                        long messageId = byteBuf.readLong();
                        long timestamp = byteBuf.readLong();
                        sendResponse(response, null, new SendMessageResult(messageId, timestamp));
                    } else {
                        sendResponse(response, errorCode, null);
                    }
                });
                return false;
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
