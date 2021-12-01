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
import cn.wildfirechat.pojos.OutputGetChannelInfo;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Get_Channel_Info)
@HttpMethod("POST")
public class GetChannelAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputChannelId inputCreateChannel = getRequestBody(request.getNettyRequest(), InputChannelId.class);
            if (inputCreateChannel != null
                && !StringUtil.isNullOrEmpty(inputCreateChannel.channelId)) {
                WFCMessage.ChannelInfo channelInfo = messagesStore.getChannelInfo(inputCreateChannel.channelId);
                if (channelInfo == null) {
                    setResponseContent(RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST), response);
                } else {
                    setResponseContent(RestResult.ok(OutputGetChannelInfo.fromPbInfo(channelInfo)), response);
                }
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
