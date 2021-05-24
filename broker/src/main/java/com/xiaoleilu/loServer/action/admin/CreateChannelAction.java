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
import cn.wildfirechat.pojos.*;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.IMTopic;

@Route(APIPath.Create_Channel)
@HttpMethod("POST")
public class CreateChannelAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputCreateChannel inputCreateChannel = getRequestBody(request.getNettyRequest(), InputCreateChannel.class);
            if (inputCreateChannel != null
                && !StringUtil.isNullOrEmpty(inputCreateChannel.getName())
                && !StringUtil.isNullOrEmpty(inputCreateChannel.getOwner())) {


                if(StringUtil.isNullOrEmpty(inputCreateChannel.getTargetId())) {
                    inputCreateChannel.setTargetId(messagesStore.getShortUUID());
                }

                sendApiMessage(response, inputCreateChannel.getOwner(), IMTopic.CreateChannelTopic, inputCreateChannel.toProtoChannelInfo().toByteArray(), result -> {
                    ByteBuf byteBuf = Unpooled.buffer();
                    byteBuf.writeBytes(result);
                    ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                    if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                        byte[] data = new byte[byteBuf.readableBytes()];
                        byteBuf.readBytes(data);
                        String channelId = new String(data);
                        return new Result(ErrorCode.ERROR_CODE_SUCCESS, new OutputCreateChannel(channelId));
                    } else {
                        return new Result(errorCode);
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
