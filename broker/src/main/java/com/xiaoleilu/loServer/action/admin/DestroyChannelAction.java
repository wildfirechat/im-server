/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputChannelId;
import cn.wildfirechat.pojos.OutputCreateChannel;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.IMTopic;

@Route(APIPath.Destroy_Channel)
@HttpMethod("POST")
public class DestroyChannelAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputChannelId inputCreateChannel = getRequestBody(request.getNettyRequest(), InputChannelId.class);
            if (inputCreateChannel != null
                && !StringUtil.isNullOrEmpty(inputCreateChannel.channelId)) {
                WFCMessage.ChannelInfo channelInfo = messagesStore.getChannelInfo(inputCreateChannel.channelId);
                if(channelInfo == null) {
                    setResponseContent(RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST), response);
                    return true;
                }

                WFCMessage.IDBuf.Builder builder = WFCMessage.IDBuf.newBuilder().setId(inputCreateChannel.channelId);
                sendApiMessage(response, channelInfo.getOwner(), IMTopic.DestroyChannelInfoTopic, builder.build().toByteArray(), result -> {
                    ByteBuf byteBuf = Unpooled.buffer();
                    byteBuf.writeBytes(result);
                    ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                    return new Result(errorCode);
                });
                return false;
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
