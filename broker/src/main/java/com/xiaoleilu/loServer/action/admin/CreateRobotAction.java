/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputCreateRobot;
import com.xiaoleilu.loServer.pojos.OutputCreateRobot;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.UUIDGenerator;

@Route("admin/robot/create")
@HttpMethod("POST")
public class CreateRobotAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputCreateRobot inputCreateRobot = getRequestBody(request.getNettyRequest(), InputCreateRobot.class);
            if (inputCreateRobot != null
                && !StringUtil.isNullOrEmpty(inputCreateRobot.getName())) {

                if(StringUtil.isNullOrEmpty(inputCreateRobot.getPassword())) {
                    inputCreateRobot.setPassword(UUIDGenerator.getUUID());
                }

                if(StringUtil.isNullOrEmpty(inputCreateRobot.getUserId())) {
                    inputCreateRobot.setUserId(messagesStore.getShortUUID());
                }

                if (inputCreateRobot.getPortrait() == null || inputCreateRobot.getPortrait().length() == 0) {
                    inputCreateRobot.setPortrait("https://avatars.io/gravatar/" + inputCreateRobot.getUserId());
                }

                WFCMessage.User newUser = inputCreateRobot.toUser();


                messagesStore.addUserInfo(newUser, inputCreateRobot.getPassword());

                if (StringUtil.isNullOrEmpty(inputCreateRobot.getOwner())) {
                    inputCreateRobot.setOwner(inputCreateRobot.getUserId());
                }

                if (StringUtil.isNullOrEmpty(inputCreateRobot.getSecret())) {
                    inputCreateRobot.setSecret(UUIDGenerator.getUUID());
                }

                messagesStore.addRobot(inputCreateRobot.toRobot());

                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok(new OutputCreateRobot(inputCreateRobot.getUserId(), inputCreateRobot.getSecret()));
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
