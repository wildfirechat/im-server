/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.InputChannelSubscribe;
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputStringList;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.ArrayList;

@Route(APIPath.Channel_Is_Subscriber)
@HttpMethod("POST")
public class GetIsChannelSubscriberAction extends ChannelAction {

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputUserId input = getRequestBody(request.getNettyRequest(), InputUserId.class);
            boolean isSubscriber = messagesStore.checkUserInChannel(input.getUserId(), channelInfo.getTargetId());
            setResponseContent(RestResult.ok(isSubscriber), response);
        }
        return true;
    }
}
