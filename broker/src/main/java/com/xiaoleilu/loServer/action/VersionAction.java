/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.InputUserLogin;
import cn.wildfirechat.pojos.OutputLoginData;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import win.liyufan.im.GitRepositoryState;
import win.liyufan.im.Utility;

import java.io.IOException;

@Route("/api/version")
@HttpMethod("GET")
public class VersionAction extends Action {


    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            response.setStatus(HttpResponseStatus.OK);

            try {

                response.setContent(Utility.formatJson(new Gson().toJson(GitRepositoryState.getGitRepositoryState())));
            } catch (IOException e) {
                e.printStackTrace();
                response.setContent("{\"version\":\"unknown\"}");
            }
        }
        return true;
    }
}
