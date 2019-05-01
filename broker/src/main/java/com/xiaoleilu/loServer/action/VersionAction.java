/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputUserLogin;
import com.xiaoleilu.loServer.pojos.OutputLoginData;
import io.moquette.spi.impl.Utils;
import io.moquette.spi.impl.security.TokenAuthenticator;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.GitRepositoryState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Route("/api/version")
@HttpMethod("GET")
public class VersionAction extends Action {


    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            response.setStatus(HttpResponseStatus.OK);

            try {
                response.setContent(new Gson().toJson(GitRepositoryState.getGitRepositoryState()));
            } catch (IOException e) {
                e.printStackTrace();
                response.setContent("{\"version\":\"unknown\"}");
            }
        }
        return true;
    }
}
