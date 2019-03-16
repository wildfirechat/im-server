/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence.remote;

import io.moquette.persistence.RPCCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RequestInfo {
    private static final Logger LOG = LoggerFactory.getLogger(RequestInfo.class);
    public final RPCCenter.Callback callback;
    final byte[] message;
    final int requestId;
    final String fromUser;
    final String request;
    final String clientId;
    public final ScheduledFuture future;

    public RequestInfo(String fromUser, String clientId, RPCCenter.Callback callback, byte[] message, int requestId, String request) {
        this.callback = callback;
        this.message = message;
        this.requestId = requestId;
        this.fromUser = fromUser;
        this.clientId = clientId;
        this.request = request;
        this.future = RPCCenter.getInstance().scheduledExecutorService.schedule(() -> {
            RequestInfo info = RPCCenter.getInstance().requestMap.remove(requestId);
            if (info != null) {
                LOG.error("Request timeout. fromUser {}, cliendId {}, requestId {}, request {}", fromUser, clientId, requestId, request);
                info.callback.getResponseExecutor().execute(() -> {
                    info.callback.onTimeout();
                });
            }
        }, 5, TimeUnit.SECONDS);
    }
}
