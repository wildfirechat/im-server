/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.client;

import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.transport.*;
import org.fusesource.mqtt.codec.CONNECT;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.fusesource.hawtbuf.Buffer.utf8;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MQTT {

    private static final long KEEP_ALIVE = Long.parseLong(System.getProperty("mqtt.thread.keep_alive", Integer.toString(1000)));
    private static final long STACK_SIZE = Long.parseLong(System.getProperty("mqtt.thread.stack_size", Integer.toString(1024*512)));
    private static ThreadPoolExecutor blockingThreadPool;


    public synchronized static ThreadPoolExecutor getBlockingThreadPool() {
        if( blockingThreadPool == null ) {
            blockingThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, KEEP_ALIVE, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread rc = new Thread(null, r, "MQTT Task", STACK_SIZE);
                        rc.setDaemon(true);
                        return rc;
                    }
                }) {

                    @Override
                    public void shutdown() {
                        // we don't ever shutdown since we are shared..
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        // we don't ever shutdown since we are shared..
                        return Collections.emptyList();
                    }
                };
        }
        return blockingThreadPool;
    }
    public synchronized static void setBlockingThreadPool(ThreadPoolExecutor pool) {
        blockingThreadPool = pool;
    }
    
    private static final URI DEFAULT_HOST = createDefaultHost();
    private static URI createDefaultHost() {
        try {
            return new URI("tcp://127.0.0.1:1883");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    protected URI host = DEFAULT_HOST;
    protected URI localAddress;
    protected SSLContext sslContext;
    protected DispatchQueue dispatchQueue;
    protected Executor blockingExecutor;
    protected int maxReadRate;
    protected int maxWriteRate;
    protected int trafficClass = TcpTransport.IPTOS_THROUGHPUT;
    protected int receiveBufferSize = 1024*64;
    protected int sendBufferSize = 1024*64;
    protected boolean useLocalHost = true;
    protected CONNECT connect = new CONNECT();

    protected long reconnectDelay = 10;
    protected long reconnectDelayMax = 30*1000;
    protected double reconnectBackOffMultiplier = 2.0f;
    protected long reconnectAttemptsMax = -1;
    protected long connectAttemptsMax = -1;
    protected Tracer tracer = new Tracer();

    public MQTT() {
    }
    public MQTT(MQTT other) {
        this.host = other.host;
        this.localAddress = other.localAddress;
        this.sslContext = other.sslContext;
        this.dispatchQueue = other.dispatchQueue;
        this.blockingExecutor = other.blockingExecutor;
        this.maxReadRate = other.maxReadRate;
        this.maxWriteRate = other.maxWriteRate;
        this.trafficClass = other.trafficClass;
        this.receiveBufferSize = other.receiveBufferSize;
        this.sendBufferSize = other.sendBufferSize;
        this.useLocalHost = other.useLocalHost;
        this.connect = new CONNECT(other.connect);
        this.reconnectDelay = other.reconnectDelay;
        this.reconnectDelayMax = other.reconnectDelayMax;
        this.reconnectBackOffMultiplier = other.reconnectBackOffMultiplier;
        this.reconnectAttemptsMax = other.reconnectAttemptsMax;
        this.connectAttemptsMax = other.connectAttemptsMax;
        this.tracer = other.tracer;
    }

    public CallbackConnection callbackConnection() {
        if( !isCleanSession() && ( getClientId()==null || getClientId().length==0 )) {
            throw new IllegalArgumentException("The client id MUST be configured when clean session is set to false");
        }
        return new CallbackConnection(new MQTT(this));
    }
    public FutureConnection futureConnection() {
        return new FutureConnection(callbackConnection());
    }
    public BlockingConnection blockingConnection() {
        return new BlockingConnection(futureConnection());
    }

    public UTF8Buffer getClientId() {
        return connect.clientId();
    }

    public short getKeepAlive() {
        return connect.keepAlive();
    }

    public UTF8Buffer getPassword() {
        return connect.password();
    }

    public byte getType() {
        return connect.messageType();
    }

    public UTF8Buffer getUserName() {
        return connect.userName();
    }

    public UTF8Buffer getWillMessage() {
        return connect.willMessage();
    }

    public QoS getWillQos() {
        return connect.willQos();
    }

    public UTF8Buffer getWillTopic() {
        return connect.willTopic();
    }

    public boolean isCleanSession() {
        return connect.cleanSession();
    }

    public boolean isWillRetain() {
        return connect.willRetain();
    }

    public void setCleanSession(boolean cleanSession) {
        connect.cleanSession(cleanSession);
    }

    public void setClientId(String clientId) {
        this.setClientId(utf8(clientId));
    }
    public void setClientId(UTF8Buffer clientId) {
        connect.clientId(clientId);
    }

    public void setKeepAlive(short keepAlive) {
        connect.keepAlive(keepAlive);
    }

    public void setPassword(String password) {
        this.setPassword(utf8(password));
    }
    public void setPassword(UTF8Buffer password) {
        connect.password(password);
    }

    public void setUserName(String userName) {
        this.setUserName(utf8(userName));
    }
    public void setUserName(UTF8Buffer userName) {
        connect.userName(userName);
    }

    public void setWillMessage(String willMessage) {
        connect.willMessage(utf8(willMessage));
    }
    public void setWillMessage(UTF8Buffer willMessage) {
        connect.willMessage(willMessage);
    }

    public void setWillQos(QoS willQos) {
        connect.willQos(willQos);
    }

    public void setVersion(String version) {
        if( "3.1".equals(version) ) {
            connect.version(3);
        } else if( "3.1.1".equals(version) ) {
            connect.version(4);
        }
    }
    public String getVersion() {
        switch(connect.version()) {
            case 3: return "3.1";
            case 4: return "3.1.1";
            default: return "unknown";
        }
    }

    public void setWillRetain(boolean willRetain) {
        connect.willRetain(willRetain);
    }

    public void setWillTopic(String willTopic) {
        this.setWillTopic(utf8(willTopic));
    }
    public void setWillTopic(UTF8Buffer willTopic) {
        connect.willTopic(willTopic);
    }

    public Executor getBlockingExecutor() {
        return blockingExecutor;
    }

    public void setBlockingExecutor(Executor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
    }

    public DispatchQueue getDispatchQueue() {
        return dispatchQueue;
    }

    public void setDispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
    }

    public URI getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) throws URISyntaxException {
        this.setLocalAddress(new URI(localAddress));
    }
    public void setLocalAddress(URI localAddress) {
        this.localAddress = localAddress;
    }

    public int getMaxReadRate() {
        return maxReadRate;
    }

    public void setMaxReadRate(int maxReadRate) {
        this.maxReadRate = maxReadRate;
    }

    public int getMaxWriteRate() {
        return maxWriteRate;
    }

    public void setMaxWriteRate(int maxWriteRate) {
        this.maxWriteRate = maxWriteRate;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public URI getHost() {
        return host;
    }
    public void setHost(String host, int port) throws URISyntaxException {
        this.setHost(new URI("tcp://"+host+":"+port));
    }
    public void setHost(String host) throws URISyntaxException {
        this.setHost(new URI(host));
    }
    public void setHost(URI host) {
        this.host = host;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isUseLocalHost() {
        return useLocalHost;
    }

    public void setUseLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
    }

    public long getConnectAttemptsMax() {
        return connectAttemptsMax;
    }

    public void setConnectAttemptsMax(long connectAttemptsMax) {
        this.connectAttemptsMax = connectAttemptsMax;
    }

    public long getReconnectAttemptsMax() {
        return reconnectAttemptsMax;
    }

    public void setReconnectAttemptsMax(long reconnectAttemptsMax) {
        this.reconnectAttemptsMax = reconnectAttemptsMax;
    }

    public double getReconnectBackOffMultiplier() {
        return reconnectBackOffMultiplier;
    }

    public void setReconnectBackOffMultiplier(double reconnectBackOffMultiplier) {
        this.reconnectBackOffMultiplier = reconnectBackOffMultiplier;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public long getReconnectDelayMax() {
        return reconnectDelayMax;
    }

    public void setReconnectDelayMax(long reconnectDelayMax) {
        this.reconnectDelayMax = reconnectDelayMax;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }
}
