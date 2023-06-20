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
import cn.wildfirechat.pojos.InputGetUserInfo;
import cn.wildfirechat.pojos.InputGetUserList;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.OutputGetUserList;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.User_Get_All)
@HttpMethod("POST")
public class GetAllUserAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserList input = getRequestBody(request.getNettyRequest(), InputGetUserList.class);
            if (input != null && input.count > 0 && input.offset >= 0) {
                List<WFCMessage.User> users = messagesStore.getUserInfoList(input.count, input.offset);
                OutputGetUserList outputGetUserList = new OutputGetUserList();
                outputGetUserList.userInfoList = new ArrayList<>();
                for (WFCMessage.User user : users) {
                    outputGetUserList.userInfoList.add(InputOutputUserInfo.fromPbUser(user));
                }

                RestResult result = RestResult.ok(outputGetUserList);
                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
