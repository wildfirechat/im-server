package cn.wildfirechat.client;

import cn.wildfirechat.proto.WFCMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xiaoleilu.loServer.pojos.InputRoute;
import io.moquette.spi.impl.security.AES;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;

import static cn.wildfirechat.client.IMClient.ConnectionStatus.ConnectionStatus_Connected;
import static cn.wildfirechat.client.IMClient.ConnectionStatus.ConnectionStatus_Connecting;
import static cn.wildfirechat.client.IMClient.ConnectionStatus.ConnectionStatus_Unconnected;


public class IMClient implements Listener {
    private final String userId;
    private final String token;
    private final String clientId;
    private final String host;
    private final int port;
    private ConnectionStatusCallback connectionStatusCallback;
    private ReceiveMessageCallback receiveMessageCallback;
    private ConnectionStatus connectionStatus;

    protected String mqttServerIp;
    protected long mqttServerPort;
    private static byte[] commonSecret= {0x00,0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x78,0x79,0x7A,0x7B,0x7C,0x7D,0x7E,0x7F};
    protected String privateSecret;

    private long messageHead;

    private transient MQTT mqtt = new MQTT();
    private transient CallbackConnection connection = null;

    public interface ReceiveMessageCallback {
        void onReceiveMessages(List<WFCMessage.Message> messageList, boolean hasMore);
        void onRecallMessage(long messageUid);
    }

    public interface ConnectionStatusCallback {
        void onConnectionStatusChanged(ConnectionStatus newStatus);
    }

    public interface SendMessageCallback {
        void onSuccess(long messageUid, long timestamp);
        void onFailure(int errorCode);
    }

    public enum ConnectionStatus {
        ConnectionStatus_Unconnected,
        ConnectionStatus_Connecting,
        ConnectionStatus_Connected,
    }

    public IMClient(String userId, String token, String clientId, String host, int port) {
        this.userId = userId;

        byte[] data = Base64.getDecoder().decode(token);
        data = AES.AESDecrypt(data, commonSecret, false);
        String s = new String(data);
        String[] ss = s.split("\\|");

        this.token = ss[0];
        this.privateSecret = ss[1];
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        AES.init(commonSecret);
    }


    public void connect() {
        if(route(userId, token)) {
            try {
                mqtt.setHost("tcp://" + mqttServerIp + ":" + mqttServerPort);
                mqtt.setVersion("3.1.1");
                mqtt.setKeepAlive((short)180);

                mqtt.setClientId(clientId);
                mqtt.setConnectAttemptsMax(100);
                mqtt.setReconnectAttemptsMax(100);

                mqtt.setUserName(userId);
                byte[] password = AES.AESEncrypt(token, privateSecret);
                mqtt.setPassword(new UTF8Buffer(password));


                connection = mqtt.callbackConnection();
                connection.listener(this);

                //connecting
                connectionStatus = ConnectionStatus_Connecting;
                if(connectionStatusCallback != null) {
                    connectionStatusCallback.onConnectionStatusChanged(connectionStatus);
                }

                connection.connect(new Callback<byte[]>() {
                    @Override
                    public void onSuccess(byte[] value) {
                        if (value != null) {
                            try {
                                WFCMessage.ConnectAckPayload ackPayload = WFCMessage.ConnectAckPayload.parseFrom(value);
                                System.out.println(ackPayload.getMsgHead());
                                messageHead = ackPayload.getMsgHead();
                            } catch (InvalidProtocolBufferException e) {
                                e.printStackTrace();
                            }

                        }

                        System.out.println("on connect success");
                        //connected
                        connectionStatus = ConnectionStatus_Connected;
                        if(connectionStatusCallback != null) {
                            connectionStatusCallback.onConnectionStatusChanged(connectionStatus);
                        }
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        System.out.println("on connect failure");
                        connectionStatus = ConnectionStatus_Unconnected;
                        if(connectionStatusCallback != null) {
                            connectionStatusCallback.onConnectionStatusChanged(connectionStatus);
                        }
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect(boolean clearSession, final Callback<Void> onComplete) {
        this.connection.disconnect(clearSession, onComplete);
    }
    public void sendMessage(WFCMessage.Conversation conversation, WFCMessage.MessageContent messageContent, final SendMessageCallback callback) {
        WFCMessage.Message message = WFCMessage.Message.newBuilder().setConversation(conversation).setContent(messageContent).setFromUser(userId).build();
        byte[] data = message.toByteArray();
        data = AES.AESEncrypt(data, privateSecret);
        connection.publish("MS", data, QoS.AT_LEAST_ONCE, false, new Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] value) {
                if (value[0] == 0) {
                    byte[] data = new byte[value.length-1];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = value[i+1];
                    }

                    data = AES.AESDecrypt(data, privateSecret, true);
                    ByteBuffer buffer = ByteBuffer.wrap(data, 0,16);

                    long messageUid = buffer.getLong();
                    long timestamp = buffer.getLong();
                    callback.onSuccess(messageUid, timestamp);
                } else {
                    callback.onFailure(value[0]);
                }
            }

            @Override
            public void onFailure(Throwable value) {
                callback.onFailure(-1);
            }
        });
    }

    protected boolean route(String userId, String token) {
        HttpPost httpPost;
        try{
            HttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost("http://" + host + ":" + port + "/route");
            InputRoute inputRoute = new InputRoute();
            inputRoute.setUserId(userId);
            inputRoute.setClientId(clientId);
            inputRoute.setToken(token);

            WFCMessage.RouteRequest routeRequest = WFCMessage.RouteRequest.newBuilder().setPlatform(0).build();

            WFCMessage.IMHttpWrapper request = WFCMessage.IMHttpWrapper.newBuilder().setClientId(clientId).setToken(token).setRequest("ROUTE").setData(ByteString.copyFrom(routeRequest.toByteArray())).build();
            byte[] data = AES.AESEncrypt(request.toByteArray(), privateSecret);
            data = Base64.getEncoder().encode(data);

            StringEntity entity = new StringEntity(new String(data), Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);

            byte[] cidByte = AES.AESEncrypt(clientId.getBytes(), commonSecret);
            cidByte = Base64.getEncoder().encode(cidByte);
            String cid = new String(cidByte);
            httpPost.setHeader("cid", cid);

            byte[] uidByte = AES.AESEncrypt(userId.getBytes(), commonSecret);
            uidByte = Base64.getEncoder().encode(uidByte);
            String uid = new String(uidByte);
            httpPost.setHeader("uid", uid);


            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Http response error {" + response.getStatusLine().getStatusCode() + "}");
                    return false;
                }
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){
                    try {
                        byte[] bytes = new byte[resEntity.getContent().available()];
                        resEntity.getContent().read(bytes);
                        if (bytes[0] == 0) {
                            byte[] bytes1 = new byte[bytes.length -1];
                            for (int i = 0; i < bytes1.length; i++) {
                                bytes1[i] = bytes[i+1];
                            }
                            byte[] rawData = AES.AESDecrypt(bytes1, privateSecret, true);
                            WFCMessage.RouteResponse routeResponse = WFCMessage.RouteResponse.parseFrom(rawData);
                            mqttServerIp = routeResponse.getHost();
                            mqttServerPort = routeResponse.getLongPort();
                            routeResponse.getShortPort();
                            return true;
                        } else {
                            System.out.println("the route failure:" + bytes[0]);
                            return false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (UnsupportedOperationException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Http response nil");
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void onConnected() {
        System.out.println("onConnected");
        connectionStatus = ConnectionStatus_Connecting;
        if(connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(connectionStatus);
        }
    }

    @Override
    public void onDisconnected() {
        System.out.println("onDisconnected");
        connectionStatus = ConnectionStatus_Unconnected;
        if(connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(connectionStatus);
        }
    }

    @Override
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
        System.out.println("onPublish" + topic.toString());
        ack.run();
        if (topic.toString().equals("MN")) {
            try {
                WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage.parseFrom(body.toByteArray());
                WFCMessage.PullMessageRequest request = WFCMessage.PullMessageRequest.newBuilder().setId(messageHead).setType(notifyMessage.getType()).build();
                connection.publish("MP", request.toByteArray(), QoS.AT_LEAST_ONCE, false, new Callback<byte[]>(){
                    @Override
                    public void onSuccess(byte[] value) {
                        try {
                            byte[] data = AES.AESDecrypt(value, privateSecret, true);
                            try {
                                WFCMessage.PullMessageResult result = WFCMessage.PullMessageResult.parseFrom(data);
                                if (receiveMessageCallback != null) {
                                    receiveMessageCallback.onReceiveMessages(result.getMessageList(), false);
                                }
                                messageHead = result.getHead();
                            } catch (InvalidProtocolBufferException e) {
                                e.printStackTrace();
                            }

                            System.out.println("onSuccess");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        System.out.println("onFailure");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onFailure(Throwable value) {
        System.out.println("onDisconnected" + value.toString());
        if(connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(ConnectionStatus_Unconnected);
        }
    }

    public void setConnectionStatusCallback(ConnectionStatusCallback connectionStatusCallback) {
        this.connectionStatusCallback = connectionStatusCallback;
    }

    public void setReceiveMessageCallback(ReceiveMessageCallback receiveMessageCallback) {
        this.receiveMessageCallback = receiveMessageCallback;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public static void main(String[] args) {
        //token与userid和clientid是绑定的，使用时一定要传入正确的userid和clientid，不然会认为token非法
        IMClient client = new IMClient("yzyOyOKK", "7SJk13q+YdHHe6EwDzry9BKogxTNf3UgtYj50cBTZgWNkNuxEkiqg2koKg0lXViONIX1LmwCR1jN0Mw8hvk6KGpiSKFi+IRaRkIb3mNzgIfrq4afhyIHaQfa2HOfsi6Ws+9YobkdDgdq7W70bEdVfiCSU9+JOIY449nxZzfg2Zw=", "DD72C212-26C7-4B38-A5FC-88550896B170", "192.168.1.101", 80);

        client.setReceiveMessageCallback(new ReceiveMessageCallback() {
            @Override
            public void onReceiveMessages(List<WFCMessage.Message> messageList, boolean hasMore) {

            }

            @Override
            public void onRecallMessage(long messageUid) {

            }
        });

        client.setConnectionStatusCallback((ConnectionStatus newStatus) -> {
            if (newStatus == ConnectionStatus_Connected) {
                WFCMessage.Conversation conversation = WFCMessage.Conversation.newBuilder().setType(0).setTarget("yzyOyOKK").setLine(0).build();
                WFCMessage.MessageContent messageContent = WFCMessage.MessageContent.newBuilder().setSearchableContent("helloworld").setType(1).build();
                client.sendMessage(conversation, messageContent, new SendMessageCallback() {
                    @Override
                    public void onSuccess(long messageUid, long timestamp) {
                        System.out.println("send success");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        System.out.println("send failure");
                    }
                });
            }
        });

        client.connect();

        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
