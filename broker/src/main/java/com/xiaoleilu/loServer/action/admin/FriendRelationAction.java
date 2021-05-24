/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import cn.wildfirechat.pojos.InputUpdateFriendStatusRequest;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

@Route(APIPath.Friend_Update_Status)
@HttpMethod("POST")
public class FriendRelationAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUpdateFriendStatusRequest friendAdd = getRequestBody(request.getNettyRequest(), InputUpdateFriendStatusRequest.class);
            if (friendAdd != null
                && !StringUtil.isNullOrEmpty(friendAdd.getUserId())
                && !StringUtil.isNullOrEmpty(friendAdd.getFriendUid())
            ) {
                WFCMessage.HandleFriendRequest.Builder friendRequestBuilder = WFCMessage.HandleFriendRequest.newBuilder().setTargetUid(friendAdd.getFriendUid()).setStatus(friendAdd.getStatus());
                if (!StringUtil.isNullOrEmpty(friendAdd.getExtra())) {
                    friendRequestBuilder = friendRequestBuilder.setExtra(friendAdd.getExtra());
                }
                sendApiMessage(response, friendAdd.getUserId(), IMTopic.HandleFriendRequestTopic, friendRequestBuilder.build().toByteArray(), result -> {
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
