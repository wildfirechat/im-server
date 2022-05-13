/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.robot;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputGetUserInfo;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.OutputRobot;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Robot_Get_Profile)
@HttpMethod("POST")
public class GetProfileAction extends RobotAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            WFCMessage.User user = messagesStore.getUserInfo(robot.getUid());
            RestResult result;
            if (user == null) {
                result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
            } else {
                OutputRobot outputRobot = new OutputRobot();
                outputRobot.fromUser(user);
                outputRobot.fromRobot(robot, false);
                result = RestResult.ok(outputRobot);
            }
            setResponseContent(result, response);
        }
        return true;
    }
}
