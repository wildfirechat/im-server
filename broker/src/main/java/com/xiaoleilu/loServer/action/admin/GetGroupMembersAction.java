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
import cn.wildfirechat.pojos.OutputGroupMemberList;
import cn.wildfirechat.pojos.PojoGroupInfo;
import cn.wildfirechat.pojos.PojoGroupMember;
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

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.Group_Member_List)
@HttpMethod("POST")
public class GetGroupMembersAction extends AdminAction {

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

                List<WFCMessage.GroupMember> members = new ArrayList<>();
                ErrorCode errorCode = messagesStore.getGroupMembers(inputGetGroup.getGroupId(), 0, members);


                response.setStatus(HttpResponseStatus.OK);
                RestResult result;
                if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                    result = RestResult.resultOf(errorCode);
                } else {
                    OutputGroupMemberList out = new OutputGroupMemberList();
                    out.setMembers(new ArrayList<>());
                    for (WFCMessage.GroupMember member : members) {
                        PojoGroupMember pm = new PojoGroupMember();
                        pm.setMember_id(member.getMemberId());
                        pm.setAlias(member.getAlias());
                        pm.setType(member.getType());
                        out.getMembers().add(pm);
                    }
                    result = RestResult.ok(out);
                }

                response.setContent(new Gson().toJson(result));
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }

        }
        return true;
    }
}
