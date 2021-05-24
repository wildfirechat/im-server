/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.InputGetFriendList;
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputStringList;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.model.FriendData;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.Blacklist_Get_List)
@HttpMethod("POST")
public class BlacklistGetAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUserId inputGetFriendList = getRequestBody(request.getNettyRequest(), InputUserId.class);
            List<FriendData> dataList = messagesStore.getFriendList(inputGetFriendList.getUserId(), null, 0);
            List<String> list = new ArrayList<>();
            for (FriendData data : dataList) {
                if (data.getBlacked() > 0) {
                    list.add(data.getFriendUid());
                }
            }
            setResponseContent(RestResult.ok(new OutputStringList(list)), response);
        }
        return true;
    }
}
