package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputApplicationGetUserInfo;
import cn.wildfirechat.pojos.OutputApplicationUserInfo;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

@Route(APIPath.User_Application_Get_UserInfo)
@HttpMethod("POST")
public class ApplicationGetUserInfoAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputApplicationGetUserInfo inputUserToken = getRequestBody(request.getNettyRequest(), InputApplicationGetUserInfo.class);

            RestResult result;
            if (inputUserToken == null || StringUtil.isNullOrEmpty(inputUserToken.getAuthCode())) {
                result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
            } else {
                String userId = messagesStore.verifyApplicationAuthCode(inputUserToken.getAuthCode(), "admin", ProtoConstants.ApplicationType.ApplicationType_Admin);
                if(userId != null) {
                    OutputApplicationUserInfo outputVerifyApplicationUser = new OutputApplicationUserInfo();
                    outputVerifyApplicationUser.setUserId(userId);
                    WFCMessage.User user = messagesStore.getUserInfo(userId);
                    if(user != null) {
                        outputVerifyApplicationUser.setDisplayName(user.getDisplayName());
                        outputVerifyApplicationUser.setPortraitUrl(user.getPortrait());
                    }
                    result = RestResult.ok(outputVerifyApplicationUser);
                } else {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_TOKEN_ERROR);
                }
            }

            response.setStatus(HttpResponseStatus.OK);
            response.setContent(new Gson().toJson(result));
        }
        return true;
    }
}

