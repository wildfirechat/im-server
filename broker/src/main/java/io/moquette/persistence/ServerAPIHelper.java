/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import cn.wildfirechat.proto.ProtoConstants;
import io.moquette.persistence.remote.*;
import io.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.wildfirechat.common.ErrorCode;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerAPIHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ServerAPIHelper.class);
    public static final String CHECK_USER_ONLINE_REQUEST = "check_user_online";
    public static final String KICKOFF_USER_REQUEST = "kickoff_user";

    private static Server server;
    public static ConcurrentHashMap<Integer, RequestInfo> requestMap = new ConcurrentHashMap<>();
    private static AtomicInteger aiRequestId = new AtomicInteger(1);
    public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    public static void init(Server s) {
        server = s;
    }

    public interface Callback {
        void onSuccess(byte[] response);
        void onError(ErrorCode errorCode);
        void onTimeout();
        Executor getResponseExecutor();
    }

    public static void sendRequest(String fromUser, String clientId, String request, byte[] message, Callback callback, ProtoConstants.RequestSourceType requestSourceType) {
        int requestId = 0;

        if (callback != null) {
            requestId = aiRequestId.incrementAndGet();
            if (requestId == Integer.MAX_VALUE) {
                if(!aiRequestId.compareAndSet(Integer.MAX_VALUE, 1)) {
                    requestId = aiRequestId.incrementAndGet();
                }
            }
            requestMap.put(requestId, new RequestInfo(fromUser, clientId, callback, message, requestId, request));
        }

        server.onApiMessage(fromUser, clientId, message, requestId, "", request, requestSourceType);
    }

    public static void sendResponse(int errorCode, byte[] message, String toUuid, int requestId) {
        LOG.debug("send async reponse to {} with requestId {}", toUuid, requestId);
        if (requestId > 0) {
            RequestInfo info = requestMap.remove(requestId);
            LOG.debug("receive async reponse requestId {}, errorCode {}", requestId, errorCode);
            if(info != null) {
                info.future.cancel(true);
                if (info.callback != null) {
                    info.callback.getResponseExecutor().execute(() -> {
                        if (errorCode == 0 || errorCode == ErrorCode.ERROR_CODE_SUCCESS_GZIPED.getCode()) {
                            info.callback.onSuccess(message);
                        } else {
                            info.callback.onError(ErrorCode.fromCode(errorCode));
                        }
                    });
                } else {

                }
            }
        }
    }
}
