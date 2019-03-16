package net.xmeter.samplers;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.security.AES;
import org.fusesource.mqtt.client.MQTT;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import net.xmeter.Util;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.QoS;


public class SendMessageSampler extends AbstractMQTTSampler implements ThreadListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4312341622759500786L;
	private transient static Logger logger = LoggingManager.getLoggerForClass();
	private transient MQTT mqtt = new MQTT();
	private transient CallbackConnection connection = null;
	private String payload = null;
	private QoS qos_enum = QoS.AT_MOST_ONCE;
	private String topicName = "";
	private String connKey = "";

	public String getQOS() {
		return getPropertyAsString(QOS_LEVEL, String.valueOf(QOS_0));
	}

	public void setQOS(String qos) {
		setProperty(QOS_LEVEL, qos);
	}

	public String getTopic() {
		return getPropertyAsString(TOPIC_NAME, DEFAULT_TOPIC_NAME);
	}

	public void setTopic(String topicName) {
		setProperty(TOPIC_NAME, topicName);
	}

    public void setTarget(String target) {
        setProperty(TARGET, target);
    }

    public String getTarget() {
        return getPropertyAsString(TARGET, DEFAULT_TARGET);
    }

    public void setConvType(String type) {
        setProperty(CONV_TYPE, type);
    }

    public String getConvType() {
        return getPropertyAsString(CONV_TYPE, CONV_TYPE_SINGLE);
    }

	public boolean isAddTimestamp() {
		return getPropertyAsBoolean(ADD_TIMESTAMP);
	}

	public void setAddTimestamp(boolean addTimestamp) {
		setProperty(ADD_TIMESTAMP, addTimestamp);
	}

	public String getMessageType() {
		return getPropertyAsString(MESSAGE_TYPE, MESSAGE_TYPE_RANDOM_STR_WITH_FIX_LEN);
	}

	public void setMessageType(String messageType) {
		setProperty(MESSAGE_TYPE, messageType);
	}

	public String getMessageLength() {
		return getPropertyAsString(MESSAGE_FIX_LENGTH, DEFAULT_MESSAGE_FIX_LENGTH);
	}

	public void setMessageLength(String length) {
		setProperty(MESSAGE_FIX_LENGTH, length);
	}

	public String getMessage() {
		return getPropertyAsString(MESSAGE_TO_BE_SENT, "hello");
	}

	public void setMessage(String message) {
		setProperty(MESSAGE_TO_BE_SENT, message);
	}

	public String getConnPrefix() {
		return getPropertyAsString(CONN_CLIENT_ID_PREFIX, DEFAULT_CONN_PREFIX_FOR_PUB);
	}
	
	public static byte[] hexToBinary(String hex) {
	    return DatatypeConverter.parseHexBinary(hex);
	}
	
	@Override
	public boolean isConnectionShareShow() {
		return true;
	}
	
	private String getKey() {
		String key = getThreadName();
		if(!isConnectionShare()) {
			key = new String(getThreadName() + this.hashCode());
		}
		return key;
	}
	
	@Override
	public SampleResult sample(Entry arg0) {
		this.connKey = getKey();
		if(connection == null) {
			connection = ConnectionsManager.getInstance().getConnection(connKey);
			if(connection != null) {
				logger.info("Use the shared connection: " + connection);
                ConnectionsManager.ConnectionInfo info = ConnectionsManager.getInstance().getConnectionInfo(connKey);
                setUserName(info.userName);
                token = info.token;
                privateSecret = info.privateSecrect;
                mqttServerIp = info.serverAddress;
                mqttServerPort = info.serverPort;
			} else {
				try {
                    if (!getToken(getUserNameAuth())) {
                        throw new Exception("get token failure!!!");
                    }

                    if (!route(getUserNameAuth(), token)) {
                        throw new Exception("route failure!!!");
                    }

                    mqtt.setHost("tcp://" + mqttServerIp + ":" + mqttServerPort);
                    mqtt.setVersion(getMqttVersion());
                    mqtt.setKeepAlive((short) Integer.parseInt(getConnKeepAlive()));

                    mqtt.setClientId(getClientId());
                    mqtt.setConnectAttemptsMax(Integer.parseInt(getConnAttamptMax()));
                    mqtt.setReconnectAttemptsMax(Integer.parseInt(getConnReconnAttamptMax()));

                    mqtt.setUserName(getUserNameAuth());

                    byte[] password = AES.AESEncrypt(token, privateSecret);
                    mqtt.setPassword(new UTF8Buffer(password));

					Object connLock = new Object();
					connection = ConnectionsManager.getInstance().createConnection(connKey, mqtt, new ConnectionsManager.ConnectionInfo(getUserNameAuth(), token, privateSecret, mqttServerIp, mqttServerPort));

					connection.listener(new Listener() {
                        @Override
                        public void onConnected() {

                        }

                        @Override
                        public void onDisconnected() {

                        }

                        @Override
                        public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
                            logger.log(Priority.DEBUG, "receive publish with topic:" + topic);
                            ack.run();
                        }

                        @Override
                        public void onFailure(Throwable value) {

                        }
                    });
					synchronized (connLock) {
						ConnectionCallback callback = new ConnectionCallback(connection, connLock);
						connection.connect(callback);
						connLock.wait(TimeUnit.SECONDS.toMillis(Integer.parseInt(getConnTimeout())));
						ConnectionsManager.getInstance().setConnectionStatus(connKey, callback.isConnectionSucc());
					}
				} catch (Exception e) {
					logger.log(Priority.ERROR, e.getMessage(), e);
					ConnectionsManager.getInstance().setConnectionStatus(connKey, false);
				}
			}
		}

		payload = getMessage();
		if(payload == null) {
		    payload = Util.generatePayload(120);
		}
		
		SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		
		if(!ConnectionsManager.getInstance().getConnectionStatus(connKey)) {
			result.sampleStart();
			result.setSuccessful(false);
			result.sampleEnd();
			result.setResponseMessage(MessageFormat.format("Publish failed for connection {0}.", connection));
			result.setResponseData("Publish failed becasue the connection has not been established.".getBytes());
			result.setResponseCode("500");
			return result;
		}
		try {
			byte[] toSend;
			result.sampleStart();
			
			final Object connLock = new Object();
			SendMessageCallback pubCallback = new SendMessageCallback(connLock);

            WFCMessage.Message message = WFCMessage.Message.newBuilder()
                .setConversation(WFCMessage.Conversation.newBuilder().setType(getConvType().equals(CONV_TYPE_SINGLE) ? ProtoConstants.ConversationType.ConversationType_Private : ProtoConstants.ConversationType.ConversationType_Group).setTarget(getTarget()).setLine(0).build())
                .setFromUser("")
                .setContent(WFCMessage.MessageContent.newBuilder().setType(1).setSearchableContent(getMessage()).build())
                .build();
            toSend = message.toByteArray();
            toSend = AES.AESEncrypt(toSend, privateSecret);

			//wildfire send message use Qos1 and MS topic
            topicName = "MS";
            synchronized (connLock) {
                connection.publish(topicName, toSend, QoS.AT_LEAST_ONCE, false, pubCallback);
                connLock.wait();
            }

            String sendData = "Conversation(" + getConvType() + ":" + getTarget() + "), Message(" + getMessage() + ")";
			result.sampleEnd();
			result.setSamplerData(sendData);
			result.setSentBytes(sendData.length());
			result.setLatency(result.getEndTime() - result.getStartTime());
			result.setSuccessful(pubCallback.isSuccessful());
			
			if(pubCallback.isSuccessful()) {
				result.setResponseData("Publish successfuly.".getBytes());
				result.setResponseMessage(MessageFormat.format("publish successfully for Connection {0}.", connection));
				result.setResponseCodeOK();	
			} else {
				result.setSuccessful(false);
				result.setResponseMessage(MessageFormat.format("Publish failed for connection {0}.", connection));
				result.setResponseData(("Publish failed with error code " + pubCallback.getErrorCode()).getBytes());
				result.setResponseCode("500");
			}
		} catch (Exception ex) {
			logger.log(Priority.ERROR, ex.getMessage(), ex);
			result.sampleEnd();
			result.setLatency(result.getEndTime() - result.getStartTime());
			result.setSuccessful(false);
			result.setResponseMessage(MessageFormat.format("Publish failed for connection {0}.", connection));
			result.setResponseData(ex.getMessage().getBytes());
			result.setResponseCode("500");
		}
		return result;
	}

	@Override
	public void threadStarted() {
		
	}

	@Override
	public void threadFinished() {
	    if (!isConnectionShare()) {
            if (this.connection != null) {
                final CountDownLatch cdl = new CountDownLatch(1);
                Callback<Void> cb = new Callback<Void>() {

                    @Override
                    public void onSuccess(Void value) {
                        logger.info(MessageFormat.format("The connection {0} disconneted successfully.", connection));
                        cdl.countDown();
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        logger.log(Priority.ERROR, value.getMessage(), value);
                        cdl.countDown();
                    }
                };
                logger.info("before disconnection");
                this.connection.disconnect(cb);
                logger.info("after disconnection");
                try {
                    cdl.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (ConnectionsManager.getInstance().containsConnection(connKey)) {
                ConnectionsManager.getInstance().removeConnection(connKey);
            }
        } else {
	        logger.info("share connection, not disconnect the connection");
        }
	}
    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();

        str = str.replace("-", "");
        return str;
    }
}
