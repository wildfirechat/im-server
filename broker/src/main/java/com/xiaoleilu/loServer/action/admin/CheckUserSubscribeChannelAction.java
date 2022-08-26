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
import cn.wildfirechat.pojos.InputChannelId;
import cn.wildfirechat.pojos.InputSubscribeChannel;
import cn.wildfirechat.pojos.OutputBooleanValue;
import cn.wildfirechat.pojos.OutputGetChannelInfo;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Check_User_Subscribe_Channel)
@HttpMethod("POST")
public class CheckUserSubscribeChannelAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputSubscribeChannel inputSubscribeChannel = getRequestBody(request.getNettyRequest(), InputSubscribeChannel.class);
            if (inputSubscribeChannel != null
                && !StringUtil.isNullOrEmpty(inputSubscribeChannel.getChannelId())
                && !StringUtil.isNullOrEmpty(inputSubscribeChannel.getUserId())) {
                boolean isInChannel = messagesStore.checkUserInChannel(inputSubscribeChannel.getUserId(), inputSubscribeChannel.getChannelId());
                OutputBooleanValue outputBooleanValue = new OutputBooleanValue();
                outputBooleanValue.value = isInChannel;
                setResponseContent(RestResult.ok(outputBooleanValue), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
