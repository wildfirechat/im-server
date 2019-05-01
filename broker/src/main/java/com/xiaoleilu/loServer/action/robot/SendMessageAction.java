/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.robot;


import cn.wildfirechat.proto.ProtoConstants;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.admin.AdminAction;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.SendMessageData;
import com.xiaoleilu.loServer.pojos.SendMessageResult;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.concurrent.Executor;

@Route("admin/robot/send")
@HttpMethod("POST")
public class SendMessageAction extends RobotAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            SendMessageData sendMessageData = getRequestBody(request.getNettyRequest(), SendMessageData.class);
            sendMessageData.setSender(robot.getUid());
            if (SendMessageData.isValide(sendMessageData)) {
                RPCCenter.getInstance().sendRequest(sendMessageData.getSender(), null, IMTopic.SendMessageTopic, sendMessageData.toProtoMessage().toByteArray(), sendMessageData.getSender(), TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
                    @Override
                    public void onSuccess(byte[] result) {
                        ByteBuf byteBuf = Unpooled.buffer();
                        byteBuf.writeBytes(result);
                        ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                            long messageId = byteBuf.readLong();
                            long timestamp = byteBuf.readLong();
                            sendResponse(response, null, new SendMessageResult(messageId, timestamp));
                        } else {
                            sendResponse(response, errorCode, null);
                        }
                    }

                    @Override
                    public void onError(ErrorCode errorCode) {
                        sendResponse(response, errorCode, null);
                    }

                    @Override
                    public void onTimeout() {
                        sendResponse(response, ErrorCode.ERROR_CODE_TIMEOUT, null);
                    }

                    @Override
                    public Executor getResponseExecutor() {
                        return command -> {
                            ctx.executor().execute(command);
                        };
                    }
                }, false);
                return false;
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }
        }
        return true;
    }
}
