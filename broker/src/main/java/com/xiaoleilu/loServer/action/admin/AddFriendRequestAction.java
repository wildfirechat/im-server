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
import cn.wildfirechat.pojos.InputAddFriendRequest;
import cn.wildfirechat.pojos.InputUpdateAlias;
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

@Route(APIPath.Friend_Send_Request)
@HttpMethod("POST")
public class AddFriendRequestAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputAddFriendRequest input = getRequestBody(request.getNettyRequest(), InputAddFriendRequest.class);

            if (StringUtil.isNullOrEmpty(input.getUserId()) || StringUtil.isNullOrEmpty(input.getFriendUid())) {
                sendResponse(response, ErrorCode.INVALID_PARAMETER, null);
                return true;
            }

            WFCMessage.AddFriendRequest addFriendRequest = WFCMessage.AddFriendRequest.newBuilder().setReason(input.getReason()).setTargetUid(input.getFriendUid()).build();
            RPCCenter.getInstance().sendRequest(input.getUserId(), null, IMTopic.AddFriendRequestTopic, addFriendRequest.toByteArray(), input.getUserId(), TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
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
            }, input.isForce());
            return false;
        }
        return true;
    }
}
