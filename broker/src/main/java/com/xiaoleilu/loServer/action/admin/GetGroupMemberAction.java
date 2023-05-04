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
import cn.wildfirechat.pojos.InputGetGroupMember;
import cn.wildfirechat.pojos.OutputGroupMemberList;
import cn.wildfirechat.pojos.PojoGroupMember;
import cn.wildfirechat.proto.ProtoConstants;
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

@Route(APIPath.Group_Member_Get)
@HttpMethod("POST")
public class GetGroupMemberAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetGroupMember input = getRequestBody(request.getNettyRequest(), InputGetGroupMember.class);
            if (input != null
                && (!StringUtil.isNullOrEmpty(input.getGroupId()))
                && (!StringUtil.isNullOrEmpty(input.getMemberId()))) {

                WFCMessage.GroupMember groupMember = messagesStore.getGroupMember(input.getGroupId(), input.getMemberId());

                RestResult result;
                if (groupMember == null || StringUtil.isNullOrEmpty(groupMember.getMemberId()) || groupMember.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Removed) {
                    result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_EXIST);
                } else {
                    PojoGroupMember pm = new PojoGroupMember();
                    pm.setMember_id(groupMember.getMemberId());
                    pm.setAlias(groupMember.getAlias());
                    pm.setType(groupMember.getType());
                    pm.setExtra(groupMember.getExtra());
                    pm.setCreateDt(groupMember.getCreateDt());
                    result = RestResult.ok(pm);
                }
                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
