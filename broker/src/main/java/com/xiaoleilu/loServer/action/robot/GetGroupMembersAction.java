/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.robot;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputGetGroup;
import cn.wildfirechat.pojos.OutputGroupMemberList;
import cn.wildfirechat.pojos.PojoGroupMember;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.admin.AdminAction;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

@Route(APIPath.Robot_Group_Member_List)
@HttpMethod("POST")
public class GetGroupMembersAction extends RobotAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetGroup inputGetGroup = getRequestBody(request.getNettyRequest(), InputGetGroup.class);
            String robotId = robot.getUid();
            if (inputGetGroup != null
                && (!StringUtil.isNullOrEmpty(inputGetGroup.getGroupId()))) {

                List<WFCMessage.GroupMember> members = new ArrayList<>();
                ErrorCode errorCode = messagesStore.getGroupMembers(null, inputGetGroup.getGroupId(), 0, members);
                RestResult result;
                if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                    result = RestResult.resultOf(errorCode);
                } else {
                    OutputGroupMemberList out = new OutputGroupMemberList();
                    out.setMembers(new ArrayList<>());
                    boolean isInGroup = false;
                    for (WFCMessage.GroupMember member : members) {
                        if (member.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Removed) {
                            continue;
                        }
                        if(member.getMemberId().equals(robotId)) {
                            isInGroup = true;
                        }
                        PojoGroupMember pm = new PojoGroupMember();
                        pm.setMember_id(member.getMemberId());
                        pm.setAlias(member.getAlias());
                        pm.setType(member.getType());
                        pm.setExtra(member.getExtra());
                        pm.setCreateDt(member.getCreateDt());
                        out.getMembers().add(pm);
                    }
                    if(!isInGroup) {
                        out.getMembers().clear();
                    }
                    result = RestResult.ok(out);
                }

                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
