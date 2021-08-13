/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;


import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.OutputUserBlockStatusList;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.lang.management.OperatingSystemMXBean;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

@Route(APIPath.Health)
public class HealthAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("node", "1");

            JSONObject cpuObject = new JSONObject();
            jsonObject.put("cpu", cpuObject);

            JSONObject memoryObject = new JSONObject();
            jsonObject.put("memory", memoryObject);

            JSONObject fileObject = new JSONObject();
            jsonObject.put("disk", fileObject);

            if (getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
                double load = getOperatingSystemMXBean().getSystemLoadAverage();
                cpuObject.put("load", load);
            }
            cpuObject.put("cores", Runtime.getRuntime().availableProcessors());

            memoryObject.put("free", Runtime.getRuntime().freeMemory());
            memoryObject.put("max", Runtime.getRuntime().maxMemory());
            memoryObject.put("avail", Runtime.getRuntime().totalMemory());

            File root = new File("/");
            fileObject.put("space", root.getTotalSpace());
            fileObject.put("free", root.getFreeSpace());
            fileObject.put("usable", root.getUsableSpace());

            JSONArray out = new JSONArray();
            out.add(jsonObject);
            RestResult result = RestResult.ok(out);
            setResponseContent(result, response);
        }
        return true;
    }
}
