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
import cn.wildfirechat.common.IMExceptionEvent;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.InputUpdateUserInfo;
import cn.wildfirechat.pojos.OutputCreateUser;
import cn.wildfirechat.proto.ProtoConstants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.UUIDGenerator;
import win.liyufan.im.Utility;

@Route(APIPath.Update_User)
@HttpMethod("POST")
public class UpdateUserAction extends AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateUserAction.class);

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUpdateUserInfo inputCreateUser = getRequestBody(request.getNettyRequest(), InputUpdateUserInfo.class);
            if (inputCreateUser != null
                && inputCreateUser.userInfo != null
                && !StringUtil.isNullOrEmpty(inputCreateUser.userInfo.getUserId())
                && inputCreateUser.flag > 0) {

                ErrorCode errorCode = messagesStore.updateUserInfo(inputCreateUser.userInfo, inputCreateUser.flag);
                setResponseContent(RestResult.resultOf(errorCode), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
