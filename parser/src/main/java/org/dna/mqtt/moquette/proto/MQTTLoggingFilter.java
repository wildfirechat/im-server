package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.AbstractIoBuffer;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache MINA logging filter that pretty prints the header of messages
 *
 * @author andrea
 */
public class MQTTLoggingFilter extends LoggingFilter {

    private final Logger log;

    protected static String computeLoggerName(String name) {
        return name == null ? MQTTLoggingFilter.class.getName() : name;
    }

    /**
     * Default Constructor.
     */
    public MQTTLoggingFilter() {
        this(LoggingFilter.class.getName());
    }

    /**
     * Create a new NoopFilter using a class name
     *
     * @param clazz the class which name will be used to create the logger
     */
    public MQTTLoggingFilter(Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * Create a new NoopFilter using a name
     *
     * @param name the name used to create the logger. If null, will default to
     * "NoopFilter"
     */
    public MQTTLoggingFilter(String name) {
        super(computeLoggerName(name));
        log = LoggerFactory.getLogger(getName());
    }

    protected String decodeCommonHeader(IoBuffer in) {
        StringBuilder sb = new StringBuilder();

        byte h1 = in.get();
        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        boolean dupFlag = ((byte) ((h1 & 0x0008) >> 3) == 1);
        byte qosLevel = (byte) ((h1 & 0x0006) >> 1);
        boolean retainFlag = ((byte) (h1 & 0x0001) == 1);
        int remainingLength = Utils.decodeRemainingLenght(in);

        sb.append("type: ").append(decodeMessageType(messageType)).append(", ");
        sb.append("dup: ").append(Boolean.toString(dupFlag)).append(", ");
        sb.append("QoS: ").append(qosLevel).append(", ");
        sb.append("retain: ").append(Boolean.toString(retainFlag));
        
        if (remainingLength != -1) {
            sb.append(", remainingLen: ").append(remainingLength);
        }

        in.rewind();
        return sb.toString();
    }

    protected String decodeMessageType(byte type) {
        switch (type) {
            case CONNECT:
                return "CONNECT";
            case CONNACK:
                return "CONNACK";
            case PUBLISH:
                return "PUBLISH";
            case PUBACK:
                return "PUBACK";
            case PUBREC:
                return "PUBREC";
            case PUBREL:
                return "PUBREL";
            case PUBCOMP:
                return "PUBCOMP";
            case SUBSCRIBE:
                return "SUBSCRIBE";
            case SUBACK:
                return "SUBACK";
            case UNSUBSCRIBE:
                return "UNSUBSCRIBE";
            case UNSUBACK:
                return "UNSUBACK";
            case PINGREQ:
                return "PINGREQ";
            case PINGRESP:
                return "PINGRESP";
            case DISCONNECT:
                return "DISCONNECT";
            default:
                return "undefined";
        }
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {

        if (message instanceof AbstractIoBuffer) {
            IoBuffer buff = (AbstractIoBuffer) message;
            log(getMessageReceivedLogLevel(), "RECEIVED: {}", decodeCommonHeader(buff));
        } else {
            log(getMessageReceivedLogLevel(), "RECEIVED: {}", message);
        }
        nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        log(getMessageSentLogLevel(), "SENT: {}", writeRequest.getMessage());
        nextFilter.messageSent(session, writeRequest);
    }

    /**
     * Copied by parent
     */
    private void log(LogLevel eventLevel, String message, Object param) {
        switch (eventLevel) {
            case TRACE:
                log.trace(message, param);
                return;
            case DEBUG:
                log.debug(message, param);
                return;
            case INFO:
                log.info(message, param);
                return;
            case WARN:
                log.warn(message, param);
                return;
            case ERROR:
                log.error(message, param);
                return;
            default:
                return;
        }
    }
}
