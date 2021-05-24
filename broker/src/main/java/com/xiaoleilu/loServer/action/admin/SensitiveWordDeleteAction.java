/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.InputOutputSensitiveWords;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.Sensitive_Del)
@HttpMethod("POST")
public class SensitiveWordDeleteAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputOutputSensitiveWords input = getRequestBody(request.getNettyRequest(), InputOutputSensitiveWords.class);
            if (input != null && input.getWords() != null && !input.getWords().isEmpty()) {
                ErrorCode errorCode = messagesStore.removeSensitiveWords(input.getWords()) ? ErrorCode.ERROR_CODE_SUCCESS : ErrorCode.ERROR_CODE_SERVER_ERROR;
                response.setStatus(HttpResponseStatus.OK);

                sendResponse(response, errorCode, null);
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
