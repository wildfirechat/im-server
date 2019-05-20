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
import com.xiaoleilu.loServer.model.FriendData;
import com.xiaoleilu.loServer.pojos.InputCreateGroup;
import com.xiaoleilu.loServer.pojos.InputFriendRequest;
import com.xiaoleilu.loServer.pojos.InputGetFriendList;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Route("admin/friend/list")
@HttpMethod("POST")
public class FriendRelationGetAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetFriendList inputGetFriendList = getRequestBody(request.getNettyRequest(), InputGetFriendList.class);
            List<FriendData> dataList = messagesStore.getFriendList(inputGetFriendList.getUserId(), 0);
            List<String> list = new ArrayList<>();
            for (FriendData data : dataList) {
                if (data.getState() == inputGetFriendList.getStatus()) {
                    list.add(data.getFriendUid());
                }
            }
            response.setStatus(HttpResponseStatus.OK);
            RestResult result = RestResult.ok(list);
            response.setContent(new Gson().toJson(result));
        }
        return true;
    }
}
