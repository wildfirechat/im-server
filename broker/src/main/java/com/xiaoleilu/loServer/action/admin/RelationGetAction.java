/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputStringList;
import cn.wildfirechat.pojos.RelationPojo;
import cn.wildfirechat.pojos.StringPairPojo;
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
import java.util.Comparator;
import java.util.List;

@Route(APIPath.Relation_Get)
@HttpMethod("POST")
public class RelationGetAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            StringPairPojo input = getRequestBody(request.getNettyRequest(), StringPairPojo.class);
            FriendData data = messagesStore.getFriendData(input.getFirst(), input.getSecond());

            RelationPojo out = new RelationPojo();
            out.userId = input.getFirst();
            out.targetId = input.getSecond();
            if(data != null ) {
                out.isFriend = data.getState() == 0;
                out.isBlacked = data.getBlacked() == 1;
                out.alias = data.getAlias();
                out.extra = data.getExtra();
            }
            setResponseContent(RestResult.ok(out), response);
        }
        return true;
    }
}
