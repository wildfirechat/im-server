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
import cn.wildfirechat.pojos.GetUserSessionResult;
import cn.wildfirechat.pojos.InputGetChatroomInfo;
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputStringList;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.MemorySessionStore;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.Collection;

@Route(APIPath.User_Session_List)
@HttpMethod("POST")
public class GetUserSessionsAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUserId inputUserId = getRequestBody(request.getNettyRequest(), InputUserId.class);
            String userId = inputUserId.getUserId();
            if (!StringUtil.isNullOrEmpty(userId)) {
                Collection<MemorySessionStore.Session> sessions = sessionsStore.sessionForUser(userId);
                GetUserSessionResult result = new GetUserSessionResult();
                for (MemorySessionStore.Session session : sessions) {
                    boolean isOnline = sessionsStore.isClientOnline(session.getClientID());
                    result.userSessions.add(new GetUserSessionResult.UserSession(session.getUsername(), session.getClientID(), session.getPlatform(), session.getPushType(), session.getDeviceToken(), session.getVoipDeviceToken(), isOnline));
                }

                setResponseContent(RestResult.ok(result), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
