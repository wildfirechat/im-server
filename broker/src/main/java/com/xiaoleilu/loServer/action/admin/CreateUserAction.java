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
import com.xiaoleilu.loServer.pojos.InputCreateUser;
import com.xiaoleilu.loServer.pojos.OutputCreateUser;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.UUIDGenerator;

@Route("admin/user/create")
@HttpMethod("POST")
public class CreateUserAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputCreateUser inputCreateUser = getRequestBody(request.getNettyRequest(), InputCreateUser.class);
            if (inputCreateUser != null
                && !StringUtil.isNullOrEmpty(inputCreateUser.getName())) {

                if(StringUtil.isNullOrEmpty(inputCreateUser.getPassword())) {
                    inputCreateUser.setPassword(UUIDGenerator.getUUID());
                }

                if(StringUtil.isNullOrEmpty(inputCreateUser.getUserId())) {
                    inputCreateUser.setUserId(messagesStore.getShortUUID());
                }

                if (inputCreateUser.getPortrait() == null || inputCreateUser.getPortrait().length() == 0) {
                    inputCreateUser.setPortrait("https://avatars.io/gravatar/" + inputCreateUser.getUserId());
                }

                WFCMessage.User.Builder newUserBuilder = WFCMessage.User.newBuilder()
                    .setUid(StringUtil.isNullOrEmpty(inputCreateUser.getUserId()) ? "" : inputCreateUser.getUserId());
                if (inputCreateUser.getName() != null)
                    newUserBuilder.setName(inputCreateUser.getName());
                if (inputCreateUser.getDisplayName() != null)
                    newUserBuilder.setDisplayName(StringUtil.isNullOrEmpty(inputCreateUser.getDisplayName()) ? inputCreateUser.getName() : inputCreateUser.getDisplayName());
                if (inputCreateUser.getPortrait() != null)
                    newUserBuilder.setPortrait(inputCreateUser.getPortrait());
                if (inputCreateUser.getEmail() != null)
                    newUserBuilder.setEmail(inputCreateUser.getEmail());
                if (inputCreateUser.getAddress() != null)
                    newUserBuilder.setAddress(inputCreateUser.getAddress());
                if (inputCreateUser.getCompany() != null)
                    newUserBuilder.setCompany(inputCreateUser.getCompany());

                if (inputCreateUser.getSocial() != null)
                    newUserBuilder.setSocial(inputCreateUser.getSocial());


                if (inputCreateUser.getMobile() != null)
                    newUserBuilder.setMobile(inputCreateUser.getMobile());
                newUserBuilder.setGender(inputCreateUser.getGender());
                if (inputCreateUser.getExtra() != null)
                    newUserBuilder.setExtra(inputCreateUser.getExtra());

                newUserBuilder.setUpdateDt(System.currentTimeMillis());

                messagesStore.addUserInfo(newUserBuilder.build(), inputCreateUser.getPassword());

                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.ok(new OutputCreateUser(inputCreateUser.getUserId(), inputCreateUser.getName()));
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
