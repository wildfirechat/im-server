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
import cn.wildfirechat.pojos.InputGetChatroomInfo;
import cn.wildfirechat.pojos.OutputGetChatroomInfo;
import cn.wildfirechat.pojos.OutputStringList;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.UserClientEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Route(APIPath.Chatroom_GetMembers)
@HttpMethod("POST")
public class GetChatroomMembersAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetChatroomInfo getChatroomInfo = getRequestBody(request.getNettyRequest(), InputGetChatroomInfo.class);
            String chatroomid = getChatroomInfo.getChatroomId();
            if (!StringUtil.isNullOrEmpty(chatroomid)) {

                Collection<UserClientEntry> members = messagesStore.getChatroomMembers(chatroomid);

                List<String> list = new ArrayList<>();
                for (UserClientEntry entry : members) {
                    list.add(entry.userId);
                }

                RestResult result;
                if (!list.isEmpty()) {
                    result = RestResult.ok(new OutputStringList(list));
                } else {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                }
                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
