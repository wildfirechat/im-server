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
import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import win.liyufan.im.IMTopic;

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
            sendApiMessage(response, input.getUserId(), IMTopic.AddFriendRequestTopic, addFriendRequest.toByteArray(), result -> {
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(result);
                ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                return new Result(errorCode);
            }, !input.isForce());
            return false;
        }
        return true;
    }
}
