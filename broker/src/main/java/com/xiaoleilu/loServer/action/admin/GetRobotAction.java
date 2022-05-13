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
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.InputRobotId;
import cn.wildfirechat.pojos.OutputRobot;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

@Route(APIPath.User_Get_Robot_Info)
@HttpMethod("POST")
public class GetRobotAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputRobotId inputUserId = getRequestBody(request.getNettyRequest(), InputRobotId.class);
            if (inputUserId != null
                && (!StringUtil.isNullOrEmpty(inputUserId.getRobotId()))) {

                WFCMessage.User user = messagesStore.getUserInfo(inputUserId.getRobotId());
                WFCMessage.Robot robot = messagesStore.getRobot(inputUserId.getRobotId());

                RestResult result;
                if (user == null || robot == null) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                } else {
                    OutputRobot outputRobot = new OutputRobot();
                    outputRobot.fromUser(user);
                    outputRobot.fromRobot(robot, true);
                    result = RestResult.ok(outputRobot);
                }

                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
