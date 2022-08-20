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
import cn.wildfirechat.pojos.InputChannelSubscribe;
import cn.wildfirechat.pojos.InputCreateChannel;
import cn.wildfirechat.pojos.InputSubscribeChannel;
import cn.wildfirechat.pojos.OutputCreateChannel;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.channel.ChannelAction;
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

@Route(APIPath.Subscribe_Channel)
@HttpMethod("POST")
public class ChannelSubscriberAction extends AdminAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputSubscribeChannel inputSubscribeChannel = getRequestBody(request.getNettyRequest(), InputSubscribeChannel.class);
            if (inputSubscribeChannel != null
                && !io.netty.util.internal.StringUtil.isNullOrEmpty(inputSubscribeChannel.getChannelId())
                && !io.netty.util.internal.StringUtil.isNullOrEmpty(inputSubscribeChannel.getUserId())) {

                WFCMessage.ListenChannel.Builder builder = WFCMessage.ListenChannel.newBuilder().setChannelId(inputSubscribeChannel.getChannelId()).setListen(inputSubscribeChannel.getSubscribe());
                sendApiMessage(response, inputSubscribeChannel.getUserId(), IMTopic.ChannelListenTopic, builder.build().toByteArray(), result -> {
                    ErrorCode errorCode = ErrorCode.fromCode(result[0]);
                    if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                        return new Result(ErrorCode.ERROR_CODE_SUCCESS);
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
