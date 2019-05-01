/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.OutputGetChatroomInfo;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;

@Route("/admin/chatroom")
@HttpMethod("GET")
public class GetChatroomInfoAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {

            String chatroomid = request.getParam("id");
            if (!StringUtil.isNullOrEmpty(chatroomid)) {

                WFCMessage.ChatroomInfo info = messagesStore.getChatroomInfo(chatroomid);

                RestResult result;
                if (info != null) {
                    result = RestResult.ok(new OutputGetChatroomInfo(chatroomid, messagesStore.getChatroomMemberCount(chatroomid), info));
                } else {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                }
                response.setStatus(HttpResponseStatus.OK);

                response.setContent(new Gson().toJson(result));

            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }

        }
        return true;
    }
}
