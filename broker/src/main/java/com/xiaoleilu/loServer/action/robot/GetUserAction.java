/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.robot;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputCreateUser;
import com.xiaoleilu.loServer.pojos.InputGetUserInfo;
import com.xiaoleilu.loServer.pojos.OutputCreateUser;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;

@Route("admin/robot/user_info")
@HttpMethod("POST")
public class GetUserAction extends RobotAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserInfo inputUserId = getRequestBody(request.getNettyRequest(), InputGetUserInfo.class);
            if (inputUserId != null
                && (!StringUtil.isNullOrEmpty(inputUserId.getUserId()) || !StringUtil.isNullOrEmpty(inputUserId.getName()))) {

                WFCMessage.User user = null;
                if(!StringUtil.isNullOrEmpty(inputUserId.getUserId())) {
                    user = messagesStore.getUserInfo(inputUserId.getUserId());
                } else if(!StringUtil.isNullOrEmpty(inputUserId.getName())) {
                    user = messagesStore.getUserInfoByName(inputUserId.getName());
                } else if(!StringUtil.isNullOrEmpty(inputUserId.getMobile())) {
                    user = messagesStore.getUserInfoByMobile(inputUserId.getMobile());
                }

                response.setStatus(HttpResponseStatus.OK);
                RestResult result;
                if (user == null) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                } else {
                    result = RestResult.ok(InputCreateUser.fromPbUser(user));
                }

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
