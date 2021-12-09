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
import cn.wildfirechat.pojos.IntStringPairPojo;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import static cn.wildfirechat.pojos.MyInfoType.*;

@Route(APIPath.Robot_Update_Profile)
@HttpMethod("POST")
public class UpdateProfileAction extends RobotAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            IntStringPairPojo input = getRequestBody(request.getNettyRequest(), IntStringPairPojo.class);

            WFCMessage.User.Builder builder = messagesStore.getUserInfo(robot.getUid()).toBuilder();
            boolean modified = false;
            String value = input.getStrValue() == null ? "" : input.getStrValue();
            switch (input.getIntValue()) {
                case Modify_DisplayName:
                    builder.setDisplayName(value);
                    modified = true;
                    break;
                case Modify_Gender:
                    builder.setGender(Integer.parseInt(value));
                    modified = true;
                    break;
                case Modify_Portrait:
                    builder.setPortrait(value);
                    modified = true;
                    break;
                //不允许修改电话号码，如果修改电话号码必须通过admin进行修改
//              case Modify_Mobile:
//                    builder.setMobile(value);
//                    modified = true;
//                    break;
                case Modify_Email:
                    builder.setEmail(value);
                    modified = true;
                    break;
                case Modify_Address:
                    builder.setAddress(value);
                    modified = true;
                    break;
                case Modify_Company:
                    builder.setCompany(value);
                    modified = true;
                    break;
                case Modify_Social:
                    builder.setSocial(value);
                    modified = true;
                    break;
                case Modify_Extra:
                    builder.setExtra(value);
                    modified = true;
                    break;
                default:
                    break;
            }

            RestResult result = RestResult.ok();
            if(modified) {
                try {
                    messagesStore.updateUserInfo(builder.build());
                } catch (Exception e) {
                    e.printStackTrace();
                    result.setErrorCode(ErrorCode.ERROR_CODE_SERVER_ERROR);
                }
            }
            setResponseContent(result, response);
        }
        return true;
    }
}
