/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputVerifyApplicationUserInfo;
import cn.wildfirechat.pojos.OutputVerifyApplicationUserInfo;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Channel_Verify_Application_UserInfo)
@HttpMethod("POST")
public class VerifyApplicationUserInfoAction extends ChannelAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputVerifyApplicationUserInfo inputUserToken = getRequestBody(request.getNettyRequest(), InputVerifyApplicationUserInfo.class);
            RestResult result;
            if (inputUserToken == null || StringUtil.isNullOrEmpty(inputUserToken.getToken())) {
                result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
            } else {
                String userId = messagesStore.verifyApplicationToken(inputUserToken.getToken(), channelInfo.getTargetId());
                if(userId != null) {
                    OutputVerifyApplicationUserInfo outputVerifyApplicationUser = new OutputVerifyApplicationUserInfo();
                    outputVerifyApplicationUser.setUserId(userId);
                    WFCMessage.User user = messagesStore.getUserInfo(userId);
                    if(user != null) {
                        outputVerifyApplicationUser.setDisplayName(user.getDisplayName());
                        outputVerifyApplicationUser.setPortraitUrl(user.getPortrait());
                    }
                    result = RestResult.ok(outputVerifyApplicationUser);
                } else {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_APPLICATION_TOKEN_ERROR_OR_TIMEOUT);
                }
            }

            response.setStatus(HttpResponseStatus.OK);
            response.setContent(new Gson().toJson(result));
        }
        return true;
    }
}
