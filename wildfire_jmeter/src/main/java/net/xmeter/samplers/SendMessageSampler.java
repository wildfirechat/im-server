package net.xmeter.samplers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import cn.wildfirechat.client.IMClient;
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
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.QoS;

import static cn.wildfirechat.client.IMClient.ConnectionStatus.ConnectionStatus_Connected;
import static cn.wildfirechat.client.IMClient.ConnectionStatus.ConnectionStatus_Connecting;


public class SendMessageSampler extends AbstractMQTTSampler implements ThreadListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4312341622759500786L;
	private transient static Logger logger = LoggingManager.getLoggerForClass();
	private transient MQTT mqtt = new MQTT();
	private transient IMClient imClient = null;
	private String payload = null;
	private QoS qos_enum = QoS.AT_MOST_ONCE;
	private String topicName = "";
	private String clientKey = "";

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
		this.clientKey = getKey();

        imClient = ClientManager.getInstance().getClient(clientKey);
		if(imClient == null) {
            try {
                if (!getToken(getUserNameAuth(), getClientId())) {
                    throw new Exception("get token failure!!!");
                }

                imClient = new IMClient(getUserNameAuth(), token, getClientId(), getServer(), Integer.parseInt(getPort()));

                final Object lock = new Object();


                final List<Boolean> ret = new ArrayList<>();
                imClient.setConnectionStatusCallback(new IMClient.ConnectionStatusCallback() {
                    @Override
                    public void onConnectionStatusChanged(IMClient.ConnectionStatus newStatus) {
                        if (newStatus == ConnectionStatus_Connected) {
                            synchronized (lock) {
                                ret.add(true);
                                lock.notify();
                            }
                        }
                    }
                });

                synchronized (lock) {
                    imClient.connect();
                    lock.wait(Integer.parseInt(getConnKeepTime()) * 1000);
                }

                ClientManager.getInstance().putClient(clientKey, imClient);
            } catch (Exception e) {
                logger.log(Priority.ERROR, e.getMessage(), e);
            }
		}

		payload = getMessage();
		if(payload == null) {
		    payload = Util.generatePayload(120);
		}
		
		final SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		
		if(ClientManager.getInstance().getClient(clientKey) == null) {
			result.sampleStart();
			result.setSuccessful(false);
			result.sampleEnd();
			result.setResponseMessage(MessageFormat.format("Publish failed for imClient {0}.", imClient));
			result.setResponseData("Publish failed becasue the imClient has not been established.".getBytes());
			result.setResponseCode("500");
			return result;
		}
		try {
			result.sampleStart();
			
			final Object lock = new Object();

            WFCMessage.Conversation conversation = WFCMessage.Conversation.newBuilder().setType(0).setTarget("yzyOyOKK").setLine(0).build();
            WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.newBuilder().setType(1).setSearchableContent(getMessage()).build();

            final List<Long> ret = new ArrayList<>();
            final List<Integer> error = new ArrayList<>();
            synchronized (lock) {
                imClient.sendMessage(conversation, messageContent, new IMClient.SendMessageCallback() {
                    @Override
                    public void onSuccess(long messageUid, long timestamp) {
                        System.out.println("send success");
                        ret.add(messageUid);
                        synchronized (lock) {
                            lock.notify();
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        System.out.println("send failure");
                        error.add(errorCode);
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                });
                lock.wait(30 * 1000);
            }


			result.sampleEnd();
			result.setSamplerData(getMessage());
			result.setSentBytes(getMessage().length());
			result.setLatency(result.getEndTime() - result.getStartTime());
			result.setSuccessful(!ret.isEmpty());
			
			if(result.isSuccessful()) {
				result.setResponseData("Publish successfuly.".getBytes());
				result.setResponseMessage(MessageFormat.format("publish successfully for Connection {0}.", imClient));
				result.setResponseCodeOK();	
			} else {
				result.setSuccessful(false);
				result.setResponseMessage(MessageFormat.format("Publish failed for imClient {0}.", imClient));
				result.setResponseData("Publish failed with error code " + error.get(0));
				result.setResponseCode("500");
			}
		} catch (Exception ex) {
			logger.log(Priority.ERROR, ex.getMessage(), ex);
			result.sampleEnd();
			result.setLatency(result.getEndTime() - result.getStartTime());
			result.setSuccessful(false);
			result.setResponseMessage(MessageFormat.format("Publish failed for imClient {0}.", imClient));
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
            if (this.imClient != null) {
                final CountDownLatch cdl = new CountDownLatch(1);
                Callback<Void> cb = new Callback<Void>() {

                    @Override
                    public void onSuccess(Void value) {
                        logger.info(MessageFormat.format("The imClient {0} disconneted successfully.", imClient));
                        cdl.countDown();
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        logger.log(Priority.ERROR, value.getMessage(), value);
                        cdl.countDown();
                    }
                };
                logger.info("before disconnection");
                this.imClient.disconnect(true, cb);
                logger.info("after disconnection");
                try {
                    cdl.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ClientManager.getInstance().removeClient(clientKey);
        } else {
	        logger.info("share imClient, not disconnect the imClient");
        }
	}
    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();

        str = str.replace("-", "");
        return str;
    }
}
