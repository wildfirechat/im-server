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

@Route(APIPath.Robot_Group_Member_Get)
@HttpMethod("POST")
public class GetGroupMemberAction extends RobotAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetGroupMember input = getRequestBody(request.getNettyRequest(), InputGetGroupMember.class);
            String robotId = robot.getUid();
            if (input != null
                && (!StringUtil.isNullOrEmpty(input.getGroupId()))
                && (!StringUtil.isNullOrEmpty(input.getMemberId()))) {

                List<WFCMessage.GroupMember> members = new ArrayList<>();
                ErrorCode errorCode = messagesStore.getGroupMembers(null, input.getGroupId(), 0, members);
                RestResult result;
                if (errorCode != ErrorCode.ERROR_CODE_SUCCESS) {
                    result = RestResult.resultOf(errorCode);
                } else {
                    WFCMessage.GroupMember groupMember = null;
                    boolean isInGroup = false;
                    for (WFCMessage.GroupMember member : members) {
                        if (member.getType() == ProtoConstants.GroupMemberType.GroupMemberType_Removed) {
                            continue;
                        }
                        if(member.getMemberId().equals(robotId)) {
                            isInGroup = true;
                            if(groupMember != null) {
                                break;
                            }
                        }
                        if(member.getMemberId().equals(input.getMemberId())) {
                            groupMember = member;
                            if(isInGroup) {
                                break;
                            }
                        }
                    }
                    if(!isInGroup) {
                        result = RestResult.resultOf(ErrorCode.ERROR_CODE_NOT_IN_GROUP);
                    } else {
                        if(groupMember == null) {
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
                    }
                }

                setResponseContent(result, response);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
