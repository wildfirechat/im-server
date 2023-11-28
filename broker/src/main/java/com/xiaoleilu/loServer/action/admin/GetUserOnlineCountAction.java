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
import cn.wildfirechat.pojos.GetOnlineUserCountResult;
import cn.wildfirechat.pojos.GetUserSessionResult;
import cn.wildfirechat.pojos.InputUserId;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.MemorySessionStore;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.Collection;

@Route(APIPath.User_Online_Count)
@HttpMethod("POST")
public class GetUserOnlineCountAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            int count = messagesStore.getOnlineUserCount();
            GetOnlineUserCountResult result = new GetOnlineUserCountResult();
            GetOnlineUserCountResult.Node node = new GetOnlineUserCountResult.Node();
            node.node = 1;
            node.count = count;
            result.add(node);

            setResponseContent(RestResult.ok(result), response);
        }
        return true;
    }
}
