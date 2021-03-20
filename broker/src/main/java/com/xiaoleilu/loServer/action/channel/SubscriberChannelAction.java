/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputChannelSubscribe;
import cn.wildfirechat.pojos.InputModifyChannelInfo;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.IMTopic;

import java.util.concurrent.Executor;

@Route(APIPath.Channel_Subscribe)
@HttpMethod("POST")
public class SubscriberChannelAction extends ChannelAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputChannelSubscribe input = getRequestBody(request.getNettyRequest(), InputChannelSubscribe.class);
            if (input != null && !StringUtil.isNullOrEmpty(input.getTarget())) {
                if(input.getSubscribe() > 0 && (channelInfo.getStatus() & ProtoConstants.ChannelState.Channel_State_Mask_Active_Subscribe) == 0) {
                    response.setStatus(HttpResponseStatus.OK);
                    RestResult result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_RIGHT);
                    response.setContent(new Gson().toJson(result));
                    return true;
                }
                WFCMessage.ListenChannel listenChannel = WFCMessage.ListenChannel.newBuilder().setChannelId(channelInfo.getTargetId()).setListen(input.getSubscribe()).build();
                RPCCenter.getInstance().sendRequest(input.getTarget(), null, IMTopic.ChannelListenTopic, listenChannel.toByteArray(), input.getTarget(), TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
                    @Override
                    public void onSuccess(byte[] result) {
                        ByteBuf byteBuf = Unpooled.buffer();
                        byteBuf.writeBytes(result);
                        ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                            sendResponse(response, null, null);
                        } else {
                            sendResponse(response, errorCode, null);
                        }
                    }

                    @Override
                    public void onError(ErrorCode errorCode) {
                        sendResponse(response, errorCode, null);
                    }

                    @Override
                    public void onTimeout() {
                        sendResponse(response, ErrorCode.ERROR_CODE_TIMEOUT, null);
                    }

                    @Override
                    public Executor getResponseExecutor() {
                        return command -> {
                            ctx.executor().execute(command);
                        };
                    }
                }, true);
                return false;
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }

        }
        return true;
    }
}
