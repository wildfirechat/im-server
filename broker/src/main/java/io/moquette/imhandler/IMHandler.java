/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.server.ThreadPoolExecutorWrapper;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.moquette.server.Server;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.MessagesPublisher;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static win.liyufan.im.ErrorCode.ERROR_CODE_OVER_FREQUENCY;
import static win.liyufan.im.ErrorCode.ERROR_CODE_SUCCESS;

/**
 * 请求处理接口<br>
 * 当用户请求某个Topic，则调用相应Handler的handle方法
 *
 */

abstract public class IMHandler<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(IMHandler.class);
    protected static IMessagesStore m_messagesStore = null;
    protected static ISessionsStore m_sessionsStore = null;
    protected static Server mServer = null;
    protected static MessagesPublisher publisher;
    private static ThreadPoolExecutorWrapper m_imBusinessExecutor;
    private static final RateLimiter mLimitCounter = new RateLimiter(5, 100);
    private Method parseDataMethod;
    private Class dataCls;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ActionMethod {
    }

    protected static String actionName;


    public IMHandler() {
        try {
            if (StringUtil.isNullOrEmpty(actionName)) {
                Class cls = getClass();
                while (cls.getSuperclass() != null) {
                    for (Method method : cls.getSuperclass().getDeclaredMethods()) {
                        if (method.getAnnotation(ActionMethod.class) != null) {
                            actionName = method.getName();
                            break;
                        }
                    }
                    if (StringUtil.isNullOrEmpty(actionName)) {
                        cls = cls.getSuperclass();
                    } else {
                        break;
                    }
                }
            }


            for (Method method : getClass().getDeclaredMethods()
                 ) {

                if (method.getName() == actionName && method.getParameterCount() == 6) {
                    dataCls = method.getParameterTypes()[4];
                    break;
                }
            }

            if (dataCls.getSuperclass().equals(GeneratedMessage.class)) {
                parseDataMethod = dataCls.getMethod("parseFrom", byte[].class);
            } else if (dataCls.isPrimitive()) {

            } else {

            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    private T getDataObject(byte[] bytes) throws IllegalAccessException, InvocationTargetException {
        if (parseDataMethod != null) {
            T object = (T) parseDataMethod.invoke(dataCls, bytes);
            return object;
        }

        if (dataCls == String.class) {
            String str = new String(bytes);
            return (T)str;
        }

        if (dataCls == Byte.class) {
            Byte b = bytes[0];
            return (T)b;
        }

        if (dataCls == Void.class) {
            return null;
        }


        //目前还没有需求传int的参数，这里先注释掉
        //在需要使用时，需要注意大小端的问题。
        //这里使用的示例代码是小端的，注意一定要与协议中保持一直！！！
//        if (dataCls == Integer.class) {
//            int i = bytes[0];
//            for(int index = 0; index <8; index++) {
//                i <<= 1;
//                i += bytes[index];
//            }
//            Integer object = i;
//            return (T)object;
//         }

        //json ?
        return (T)(new Gson().fromJson(new String(bytes), dataCls));
    }

    public static void init(IMessagesStore ms, ISessionsStore ss, MessagesPublisher p, ThreadPoolExecutorWrapper businessExecutor, Server server) {
        m_messagesStore = ms;
        m_sessionsStore = ss;
        publisher = p;
        m_imBusinessExecutor = businessExecutor;
        mServer = server;
    }


    public ErrorCode preAction(String clientID, String fromUser, String topic, Qos1PublishHandler.IMCallback callback) {
        LOG.info("imHandler fromUser={}, clientId={}, topic={}", fromUser, clientID, topic);
        if(!mLimitCounter.isGranted(clientID + fromUser + topic)) {
            ByteBuf ackPayload = Unpooled.buffer();
            ackPayload.ensureWritable(1).writeByte(ERROR_CODE_OVER_FREQUENCY.getCode());
            try {
                callback.onIMHandled(ERROR_CODE_OVER_FREQUENCY, ackPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e);
            }
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }
        return ErrorCode.ERROR_CODE_SUCCESS;
    }

	public void doHandler(String clientID, String fromUser, String topic, byte[] payloadContent, Qos1PublishHandler.IMCallback callback, boolean isAdmin) {
        m_imBusinessExecutor.execute(() -> {
            Qos1PublishHandler.IMCallback callbackWrapper = new Qos1PublishHandler.IMCallback() {
                @Override
                public void onIMHandled(ErrorCode errorCode, ByteBuf ackPayload) {
                    LOG.debug("execute handler {} with result {}", this.getClass().getName(), errorCode);
                    callback.onIMHandled(errorCode, ackPayload);
                    afterAction(clientID, fromUser, topic, callback);
                }
            };

            ErrorCode preActionCode = preAction(clientID, fromUser, topic, callbackWrapper);

            if (preActionCode == ErrorCode.ERROR_CODE_SUCCESS) {
                ByteBuf ackPayload = Unpooled.buffer(1);
                ErrorCode errorCode = ERROR_CODE_SUCCESS;
                ackPayload.ensureWritable(1).writeByte(errorCode.getCode());

                try {
                    LOG.debug("execute handler for topic {}", topic);
                    errorCode = action(ackPayload, clientID, fromUser, isAdmin, getDataObject(payloadContent), callbackWrapper);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_INVALID_DATA;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    errorCode = ErrorCode.ERROR_CODE_INVALID_DATA;
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    if (e instanceof InvalidProtocolBufferException) {
                        errorCode = ErrorCode.ERROR_CODE_INVALID_DATA;
                    } else {
                        errorCode = ErrorCode.ERROR_CODE_SERVER_ERROR;
                    }
                }

                if(errorCode != ErrorCode.ERROR_CODE_ASYNC_HANDLER) {
                    response(ackPayload, errorCode, callback);
                }
            } else {
                LOG.error("Handler {} preAction failure", this.getClass().getName());
                ByteBuf ackPayload = Unpooled.buffer(1);
                ackPayload.ensureWritable(1).writeByte(preActionCode.getCode());
                response(ackPayload, preActionCode, callback);
            }
        });
    }

    private void response(ByteBuf ackPayload, ErrorCode errorCode, Qos1PublishHandler.IMCallback callback) {
        ackPayload.setByte(0, errorCode.getCode());
        try {
            callback.onIMHandled(errorCode, ackPayload);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }


    @ActionMethod
    abstract public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, T request, Qos1PublishHandler.IMCallback callback)   ;

    public void afterAction(String clientID, String fromUser, String topic, Qos1PublishHandler.IMCallback callback) {

    }

    protected long saveAndPublish(String username, String clientID, WFCMessage.Message message) {
        Set<String> notifyReceivers = new LinkedHashSet<>();

        message = m_messagesStore.storeMessage(username, clientID, message);
        int pullType = m_messagesStore.getNotifyReceivers(username, message, notifyReceivers);
        this.publisher.publish2Receivers(message, notifyReceivers, clientID, pullType);
        return message.getMessageId();
    }
}
