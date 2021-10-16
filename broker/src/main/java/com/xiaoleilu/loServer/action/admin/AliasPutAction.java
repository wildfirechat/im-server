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
import cn.wildfirechat.pojos.InputGetFriendList;
import cn.wildfirechat.pojos.InputUpdateAlias;
import cn.wildfirechat.pojos.OutputCreateChannel;
import cn.wildfirechat.pojos.OutputStringList;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.model.FriendData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.IMTopic;

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.Friend_Set_Alias)
@HttpMethod("POST")
public class AliasPutAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUpdateAlias input = getRequestBody(request.getNettyRequest(), InputUpdateAlias.class);
            if(!StringUtil.isNullOrEmpty(input.getOperator()) && !StringUtil.isNullOrEmpty(input.getTargetId())) {
                WFCMessage.AddFriendRequest addFriendRequest = WFCMessage.AddFriendRequest.newBuilder().setTargetUid(input.getTargetId()).setReason(input.getAlias()==null?"":input.getAlias()).build();
                sendApiMessage(response, input.getOperator(), IMTopic.SetFriendAliasTopic, addFriendRequest.toByteArray(), result -> {
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
                ErrorCode errorCode = messagesStore.setFriendAliasRequest(input.getOperator(), input.getTargetId(), input.getAlias(), new long[1]);
                setResponseContent(RestResult.resultOf(errorCode), response);
            }
        }
        return true;
    }
}
