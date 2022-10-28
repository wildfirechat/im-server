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
import cn.wildfirechat.pojos.InputUserId;
import cn.wildfirechat.pojos.OutputGroupIds;
import cn.wildfirechat.pojos.StringPairPojo;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Set;

@Route(APIPath.Get_Common_Groups)
@HttpMethod("POST")
public class GetCommonGroupsAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            StringPairPojo input = getRequestBody(request.getNettyRequest(), StringPairPojo.class);
            if (input != null
                && (!StringUtil.isNullOrEmpty(input.getFirst())) && (!StringUtil.isNullOrEmpty(input.getSecond()))) {

                Set<String> groupIds = messagesStore.getCommonGroupIds(input.getFirst(), input.getSecond());
                OutputGroupIds outputGroupIds = new OutputGroupIds();
                outputGroupIds.setGroupIds(new ArrayList<>(groupIds));
                RestResult result = RestResult.ok(outputGroupIds);
                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
