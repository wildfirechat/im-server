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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;


public class IMClient implements Listener {
    private final String userId;
    private final String token;
    private final String clientId;
    private final String host;
    private final int port;

    protected String mqttServerIp;
    protected long mqttServerPort;
    private static byte[] commonSecret= {0x00,0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x78,0x79,0x7A,0x7B,0x7C,0x7D,0x7E,0x7F};
    protected String privateSecret;

    private long messageHead;

    public IMClient(String userId, String token, String clientId, String host, int port) {
        this.userId = userId;
        this.token = token;
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        AES.init(commonSecret);
    }

    private transient MQTT mqtt = new MQTT();
    private transient CallbackConnection connection = null;

    public void connect() {
        if(route(userId, token)) {
            try {
                mqtt.setHost("tcp://" + mqttServerIp + ":" + mqttServerPort);
                mqtt.setVersion("1.1");
                mqtt.setKeepAlive((short)180);

                mqtt.setClientId(clientId);
                mqtt.setConnectAttemptsMax(100);
                mqtt.setReconnectAttemptsMax(100);

                mqtt.setUserName(userId);
                byte[] password = AES.AESEncrypt(token, privateSecret);
                mqtt.setPassword(new UTF8Buffer(password));


                connection = mqtt.callbackConnection();
                connection.listener(this);
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
                    }

                    @Override
                    public void onFailure(Throwable value) {
                        System.out.println("on connect failure");
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(WFCMessage.Conversation conversation, WFCMessage.MessageContent messageContent) {
        WFCMessage.Message message = WFCMessage.Message.newBuilder().setConversation(conversation).setContent(messageContent).setFromUser(userId).build();
        byte[] data = message.toByteArray();
        data = AES.AESEncrypt(data, privateSecret);
        connection.publish("MS", data, QoS.AT_LEAST_ONCE, false, new Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] value) {

            }

            @Override
            public void onFailure(Throwable value) {

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

            WFCMessage.IMHttpWrapper request = WFCMessage.IMHttpWrapper.newBuilder().setClientId(clientId).setToken(token).setRequest("ROUTE").setData(ByteString.copyFrom(token.getBytes())).build();
            byte[] data = AES.AESEncrypt(request.toByteArray(), commonSecret);
            data = Base64.getEncoder().encode(data);

            StringEntity entity = new StringEntity(new String(data), Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);


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
                            byte[] rawData = AES.AESDecrypt(bytes1, "", true);
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
    }

    @Override
    public void onDisconnected() {
        System.out.println("onDisconnected");
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
    }

    public static void main(String[] args) {
        IMClient client = new IMClient("-C-3-3KK","hN0AF2XX6+o1vf6UIC4Fo5O6E+EWC2nmnZd7xZi4SlU=", "1234", "im.liyufan.win", 80);
        client.connect();
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
