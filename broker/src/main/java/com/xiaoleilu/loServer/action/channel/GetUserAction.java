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
import cn.wildfirechat.pojos.InputGetUserInfo;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Channel_User_Info)
@HttpMethod("POST")
public class GetUserAction extends ChannelAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserInfo inputUserId = getRequestBody(request.getNettyRequest(), InputGetUserInfo.class);
            if (inputUserId != null
                && (!StringUtil.isNullOrEmpty(inputUserId.getUserId()) || !StringUtil.isNullOrEmpty(inputUserId.getName()) || !StringUtil.isNullOrEmpty(inputUserId.getMobile()))) {

                WFCMessage.User user = null;
                if(!StringUtil.isNullOrEmpty(inputUserId.getUserId())) {
                    user = messagesStore.getUserInfo(inputUserId.getUserId());
                } else if(!StringUtil.isNullOrEmpty(inputUserId.getName())) {
                    user = messagesStore.getUserInfoByName(inputUserId.getName());
                } else if(!StringUtil.isNullOrEmpty(inputUserId.getMobile())) {
                    user = messagesStore.getUserInfoByMobile(inputUserId.getMobile());
                }

                RestResult result;

                if (user == null || StringUtil.isNullOrEmpty(user.getName())) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                } else {
                    if (channelInfo.getOwner().equals(inputUserId.getUserId()) ||
                            messagesStore.checkUserInChannel(user.getUid(), channelInfo.getTargetId())) {
                        if((channelInfo.getStatus() & ProtoConstants.ChannelState.Channel_State_Mask_FullInfo) > 0) {
                            result = RestResult.ok(InputOutputUserInfo.fromPbUser(user));
                        } else {
                            WFCMessage.User outUser = WFCMessage.User.newBuilder().setUid(user.getUid()).setName(user.getName()).setPortrait(user.getPortrait()).setDisplayName(user.getDisplayName()).build();
                            result = RestResult.ok(InputOutputUserInfo.fromPbUser(outUser));
                        }
                    } else {
                        if((channelInfo.getStatus() & ProtoConstants.ChannelState.Channel_State_Mask_Unsubscribed_User_Access) > 0) {
                            WFCMessage.User outUser = WFCMessage.User.newBuilder().setUid(user.getUid()).setName(user.getName()).setPortrait(user.getPortrait()).setDisplayName(user.getDisplayName()).build();
                            result = RestResult.ok(InputOutputUserInfo.fromPbUser(outUser));
                        } else {
                            result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_RIGHT);
                        }
                    }
                }

                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
