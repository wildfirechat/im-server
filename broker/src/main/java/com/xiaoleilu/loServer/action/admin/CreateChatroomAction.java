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
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.InputCreateChatroom;
import cn.wildfirechat.pojos.OutputCreateChatroom;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.Create_Chatroom)
@HttpMethod("POST")
public class CreateChatroomAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest)request.getNettyRequest();
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = new String(bytes);
            Gson gson = new Gson();
            InputCreateChatroom inputCreateChatroom = gson.fromJson(content, InputCreateChatroom.class);
            if (inputCreateChatroom != null
                && !StringUtil.isNullOrEmpty(inputCreateChatroom.getTitle())) {

                if (StringUtil.isNullOrEmpty(inputCreateChatroom.getChatroomId())) {
                    inputCreateChatroom.setChatroomId(messagesStore.getShortUUID());
                }

                if (inputCreateChatroom.getPortrait() == null || inputCreateChatroom.getPortrait().length() == 0) {
                    inputCreateChatroom.setPortrait("https://avatars.io/gravatar/" + inputCreateChatroom.getChatroomId());
                }

                WFCMessage.ChatroomInfo info = inputCreateChatroom.toChatroomInfo();
                messagesStore.createChatroom(inputCreateChatroom.getChatroomId(), info);
                setResponseContent(RestResult.ok(new OutputCreateChatroom(inputCreateChatroom.getChatroomId())), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
