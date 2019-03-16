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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Promise<T> implements Callback<T>, Future<T> {

    private final CountDownLatch latch = new CountDownLatch(1);
    private Callback<T> next;
    private Throwable error;
    private T value;

    public void onFailure(Throwable value) {
        Callback<T> callback = null;
        synchronized(this)  {
            error = value;
            latch.countDown();
            callback = next;
        }
        if( callback!=null ) {
            callback.onFailure(value);
        }
    }

    public void onSuccess(T value) {
        Callback<T> callback = null;
        synchronized(this)  {
            this.value = value;
            latch.countDown();
            callback = next;
        }
        if( callback!=null ) {
            callback.onSuccess(value);
        }
    }

    public void then(Callback<T> callback) {
        boolean fire = false;
        synchronized(this)  {
            next = callback;
            if( latch.getCount() == 0 ) {
                fire = true;
            }
        }
        if( fire ) {
            if( error!=null ) {
                callback.onFailure(error);
            } else {
                callback.onSuccess(value);
            }
        }
    }

    public T await(long amount, TimeUnit unit) throws Exception {
        if( latch.await(amount, unit) ) {
            return get();
        } else {
            throw new TimeoutException();
        }
    }

    public T await() throws Exception {
        latch.await();
        return get();
    }

    private T get() throws Exception {
        Throwable e = error;
        if( e !=null ) {
            if( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else if( e instanceof Exception) {
                throw (Exception) e;
            } else if( e instanceof Error) {
                throw (Error) e;
            } else {
                // don't expect to hit this case.
                throw new RuntimeException(e);
            }
        }
        return value;
    }

}
