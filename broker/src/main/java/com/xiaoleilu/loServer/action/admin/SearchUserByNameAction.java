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
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.PojoSearchUserByNameReq;
import cn.wildfirechat.pojos.mesh.PojoSearchUserRes;
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
 * 根据用户名称搜索用户，模糊匹配用户名和昵称
 */
@Route(APIPath.User_Search_By_Name)
@HttpMethod("POST")
public class SearchUserByNameAction extends AdminAction {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            PojoSearchUserByNameReq input = getRequestBody(request.getNettyRequest(), PojoSearchUserByNameReq.class);
            if (input != null && !StringUtil.isNullOrEmpty(input.keyword) && !StringUtil.isNullOrEmpty(input.keyword.trim())) {
                int page = Math.max(input.page, 0);
                int count = input.count <= 0 ? DEFAULT_PAGE_SIZE : Math.min(input.count, MAX_PAGE_SIZE);

                List<WFCMessage.User> users = messagesStore.searchUserByName(input.keyword.trim(), page, count);

                PojoSearchUserRes res = new PojoSearchUserRes();
                res.keyword = input.keyword;
                res.userInfos = new ArrayList<>();
                for (WFCMessage.User user : users) {
                    res.userInfos.add(InputOutputUserInfo.fromPbUser(user));
                }

                setResponseContent(RestResult.ok(res), response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }
        }
        return true;
    }
}
