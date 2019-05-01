/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputOutputSensitiveWords;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.ErrorCode;

import java.util.List;

@Route("admin/sensitive/query")
@HttpMethod("POST")
public class SensitiveWordQueryAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
                List<String> words = messagesStore.getAllSensitiveWords();
                InputOutputSensitiveWords out = new InputOutputSensitiveWords();
                out.setWords(words);
                sendResponse(response, ErrorCode.ERROR_CODE_SUCCESS, out);
        }
        return true;
    }
}
