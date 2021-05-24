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
import cn.wildfirechat.pojos.InputGetAlias;
import cn.wildfirechat.pojos.InputGetFriendList;
import cn.wildfirechat.pojos.OutputGetAlias;
import cn.wildfirechat.pojos.OutputStringList;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.model.FriendData;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.Friend_Get_Alias)
@HttpMethod("POST")
public class AliasGetAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetAlias input = getRequestBody(request.getNettyRequest(), InputGetAlias.class);
            List<FriendData> dataList = messagesStore.getFriendList(input.getOperator(), null, 0);
            List<String> list = new ArrayList<>();
            OutputGetAlias out = new OutputGetAlias(input.getOperator(), input.getTargetId());

            for (FriendData data : dataList) {
                if (data.getFriendUid().equals(input.getTargetId())) {
                    out.setAlias(data.getAlias());
                    break;
                }
            }

            setResponseContent(RestResult.ok(out), response);
        }
        return true;
    }
}
