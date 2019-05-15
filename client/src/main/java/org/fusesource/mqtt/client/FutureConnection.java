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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.fusesource.hawtbuf.Buffer.utf8;

/**
 * <p>
 * A Future based optionally-blocking Connection interface to MQTT.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureConnection {

    private final CallbackConnection next;

    private final LinkedList<Promise<Message>> receiveFutures = new LinkedList<Promise<Message>>();
    private final LinkedList<Message> receivedFrames = new LinkedList<Message>();

    private volatile boolean connected;

    private long receiveBuffer = 1024*1024;
    private long receiveBufferRemaining = receiveBuffer;
    private boolean receiveBufferFull = false;

    public FutureConnection(CallbackConnection next) {
        this.next = next;
        this.next.listener(new ExtendedListener() {

            public void onConnected() {
                connected = true;
            }

            public void onDisconnected() {
                connected = false;
            }


            public void onPublish(UTF8Buffer topic, final Buffer payload, final Callback<Callback<byte[]>> onComplete) {
                getDispatchQueue().assertExecuting();
                receiveBufferRemaining -= payload.length();
                if( !receiveBufferFull && receiveBufferRemaining <=0 ) {
                    receiveBufferFull = true;
                    suspend();
                }
                deliverMessage(new Message(getDispatchQueue(), topic, payload, new Callback<Callback<byte[]>>() {
                    public void onSuccess(Callback<byte[]> value) {
                        processed();
                        onComplete.onSuccess(value);
                    }
                    public void onFailure(Throwable value) {
                        processed();
                        onComplete.onFailure(value);
                    }

                    private void processed() {
                        getDispatchQueue().assertExecuting();
                        receiveBufferRemaining += payload.length();
                        if( receiveBufferFull && receiveBufferRemaining >0 ) {
                            receiveBufferFull = false;
                            resume();
                        }
                    }

                }));
            }

            public void onPublish(UTF8Buffer topic, Buffer payload, final Runnable onComplete) {
                onPublish(topic, payload, new Callback<Callback<byte[]>>() {
                    public void onSuccess(Callback<byte[]> value) {
                        onComplete.run();
                    }
                    public void onFailure(Throwable value) {
                    }
                });
            }

            public void onFailure(Throwable value) {
                getDispatchQueue().assertExecuting();
                ArrayList<Promise<?>> tmp = new ArrayList<Promise<?>>(receiveFutures);
                receiveFutures.clear();
                for (Promise<?> future : tmp) {
                    future.onFailure(value);
                }
                connected = false;
            }
        });
    }

    void deliverMessage(Message msg) {
        if( receiveFutures.isEmpty() ) {
            receivedFrames.add(msg);
        } else {
            receiveFutures.removeFirst().onSuccess(msg);
        }
    }

    void putBackMessage(Message msg) {
        if( receiveFutures.isEmpty() ) {
            receivedFrames.addFirst(msg);
        } else {
            receiveFutures.removeFirst().onSuccess(msg);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public DispatchQueue getDispatchQueue() {
        return this.next.getDispatchQueue();
    }


    public Future<byte[]> connect() {
        final Promise<byte[]> future = new Promise<byte[]>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.connect(future);
            }
        });
        return future;
    }

    public Future<Void> disconnect() {
        final Promise<Void> future = new Promise<Void>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.disconnect(false, future);
            }
        });
        return future;
    }

    public Future<Void> kill() {
        final Promise<Void> future = new Promise<Void>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.kill(future);
            }
        });
        return future;
    }

    public Future<byte[]> subscribe(final Topic[] topics) {
        final Promise<byte[]> future = new Promise<byte[]>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.subscribe(topics, future);
            }
        });
        return future;
    }
    
    public Future<Void> unsubscribe(final String[] topics) {
        UTF8Buffer[] buffers = new UTF8Buffer[topics.length];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new UTF8Buffer(topics[i]);
        }
        return unsubscribe(buffers);
    }

    public Future<Void> unsubscribe(final UTF8Buffer[] topics) {
        final Promise<Void> future = new Promise<Void>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.unsubscribe(topics, future);
            }
        });
        return future;
    }

    public Future<byte[]> publish(final String topic, final byte[] payload, final QoS qos, final boolean retain) {
        return publish(utf8(topic), new Buffer(payload), qos, retain);
    }

    public Future<byte[]> publish(final UTF8Buffer topic, final Buffer payload,  final QoS qos, final boolean retain) {
        final Promise<byte[]> future = new Promise<byte[]>();
        next.getDispatchQueue().execute(new Task() {
            public void run() {
                next.publish(topic, payload, qos, retain, future);
            }
        });
        return future;
    }

    public Future<Message> receive() {
        final Promise<Message> future = new Promise<Message>();
        getDispatchQueue().execute(new Task(){
            public void run() {
                if( next.failure()!=null ) {
                    future.onFailure(next.failure());
                } else {
                    if( receivedFrames.isEmpty() ) {
                        receiveFutures.add(future);
                    } else {
                        future.onSuccess(receivedFrames.removeFirst());
                    }
                }
            }
        });
        return future;
    }

    public long getReceiveBuffer() {
        getDispatchQueue().assertExecuting();
        return receiveBuffer;
    }

    public void setReceiveBuffer(long receiveBuffer) {
        getDispatchQueue().assertExecuting();
        long prev = this.receiveBuffer;
        this.receiveBuffer = receiveBuffer;
        long diff = prev-receiveBuffer;

        receiveBufferRemaining -= diff;
        if( !receiveBufferFull && receiveBufferRemaining <=0 ) {
            receiveBufferFull = true;
            suspend();
        } else if( receiveBufferFull && receiveBufferRemaining >0 ) {
            receiveBufferFull = false;
            resume();
        }
    }

    public void resume() {
        next.resume();
    }

    public void suspend() {
        next.suspend();
    }
}
