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
import cn.wildfirechat.pojos.InputGetGroup;
import cn.wildfirechat.pojos.InputGetUserInfo;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.PojoGroupInfo;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

@Route(APIPath.Group_Get_Info)
@HttpMethod("POST")
public class GetGroupInfoAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetGroup inputGetGroup = getRequestBody(request.getNettyRequest(), InputGetGroup.class);
            if (inputGetGroup != null
                && (!StringUtil.isNullOrEmpty(inputGetGroup.getGroupId()))) {

                WFCMessage.GroupInfo groupInfo = messagesStore.getGroupInfo(inputGetGroup.getGroupId());
                RestResult result;
                if (groupInfo == null) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                } else {
                    PojoGroupInfo pojoGroupInfo = new PojoGroupInfo();
                    pojoGroupInfo.setExtra(groupInfo.getExtra());
                    pojoGroupInfo.setName(groupInfo.getName());
                    pojoGroupInfo.setOwner(groupInfo.getOwner());
                    pojoGroupInfo.setPortrait(groupInfo.getPortrait());
                    pojoGroupInfo.setTarget_id(groupInfo.getTargetId());
                    pojoGroupInfo.setType(groupInfo.getType());
                    pojoGroupInfo.setMute(groupInfo.getMute());
                    pojoGroupInfo.setJoin_type(groupInfo.getJoinType());
                    pojoGroupInfo.setPrivate_chat(groupInfo.getPrivateChat());
                    pojoGroupInfo.setSearchable(groupInfo.getSearchable());
                    result = RestResult.ok(pojoGroupInfo);
                }
                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
