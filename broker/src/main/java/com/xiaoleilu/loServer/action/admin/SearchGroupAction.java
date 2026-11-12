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
import cn.wildfirechat.pojos.PojoGroupInfo;
import cn.wildfirechat.pojos.PojoSearchGroupReq;
import cn.wildfirechat.pojos.PojoSearchGroupRes;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据群组名称搜索群组
 */
@Route(APIPath.Group_Search)
@HttpMethod("POST")
public class SearchGroupAction extends AdminAction {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            PojoSearchGroupReq input = getRequestBody(request.getNettyRequest(), PojoSearchGroupReq.class);
            if (input != null && !StringUtil.isNullOrEmpty(input.keyword) && !StringUtil.isNullOrEmpty(input.keyword.trim())) {
                int page = Math.max(input.page, 0);
                int count = input.count <= 0 ? DEFAULT_PAGE_SIZE : Math.min(input.count, MAX_PAGE_SIZE);

                List<WFCMessage.GroupInfo> groups = messagesStore.searchGroup(input.keyword.trim(), page, count);

                PojoSearchGroupRes res = new PojoSearchGroupRes();
                res.keyword = input.keyword;
                res.groupInfos = new ArrayList<>();
                for (WFCMessage.GroupInfo groupInfo : groups) {
                    res.groupInfos.add(PojoGroupInfo.fromProto(groupInfo));
                }

                setResponseContent(RestResult.ok(res), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
