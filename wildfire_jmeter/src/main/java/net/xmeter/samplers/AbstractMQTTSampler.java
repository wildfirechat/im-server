package net.xmeter.samplers;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.pojos.InputRoute;
import com.xiaoleilu.loServer.pojos.InputGetToken;
import com.xiaoleilu.loServer.pojos.OutputGetIMTokenData;
import io.moquette.spi.impl.security.AES;
import net.xmeter.IMResult;
import net.xmeter.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jmeter.samplers.AbstractSampler;

import net.xmeter.Constants;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Base64;

public abstract class AbstractMQTTSampler extends AbstractSampler implements Constants {
    private transient static Logger logger = LoggingManager.getLoggerForClass();
	/**
	 * 
	 */
	private static final long serialVersionUID = 7163793218595455807L;

	public String getServer() {
		return getPropertyAsString(SERVER, DEFAULT_SERVER);
	}

	public void setServer(String server) {
		setProperty(SERVER, server);
	}
	
	public String getMqttVersion() {
		return getPropertyAsString(MQTT_VERSION, DEFAULT_MQTT_VERSION);
	}

	public String getPort() {
		return getPropertyAsString(PORT, DEFAULT_PORT);
	}

	public void setPort(String port) {
		setProperty(PORT, port);
	}

	public boolean isConnectionShare() {
		return getPropertyAsBoolean(CONN_SHARE_CONNECTION, DEFAULT_CONNECTION_SHARE);
	}
	
	public void setConnectionShare(boolean shared) {
		setProperty(CONN_SHARE_CONNECTION, shared);
	}
	
	public String getConnTimeout() {
		return getPropertyAsString(CONN_TIMEOUT, DEFAULT_CONN_TIME_OUT);
	}

	public void setConnTimeout(String connTimeout) {
		setProperty(CONN_TIMEOUT, connTimeout);
	}

	public String getProtocol() {
		return getPropertyAsString(PROTOCOL, DEFAULT_PROTOCOL);
	}

	public String getConnPrefix() {
		return getPropertyAsString(CONN_CLIENT_ID_PREFIX, DEFAULT_CONN_PREFIX_FOR_CONN);
	}

	public void setConnPrefix(String connPrefix) {
	    logger.info("set connprefix:" + connPrefix);
		setProperty(CONN_CLIENT_ID_PREFIX, connPrefix);
	}

	public String getConnKeepAlive() {
		return getPropertyAsString(CONN_KEEP_ALIVE, DEFAULT_CONN_KEEP_ALIVE);
	}

	public void setConnKeepAlive(String connKeepAlive) {
		setProperty(CONN_KEEP_ALIVE, connKeepAlive);
	}
	
	public boolean isClientIdSuffix() {
		return getPropertyAsBoolean(CONN_CLIENT_ID_SUFFIX, DEFAULT_ADD_CLIENT_ID_SUFFIX);
	}
	
	public void setClientIdSuffix(boolean clientIdSuffix) {
		setProperty(CONN_CLIENT_ID_SUFFIX, clientIdSuffix);
	}

	public String getConnKeepTime() {
		return getPropertyAsString(CONN_KEEP_TIME, DEFAULT_CONN_KEEP_TIME);
	}

	public void setConnKeepTime(String connKeepTime) {
		setProperty(CONN_KEEP_TIME, connKeepTime);
	}

	public String getConnAttamptMax() {
		return getPropertyAsString(CONN_ATTAMPT_MAX, DEFAULT_CONN_ATTAMPT_MAX);
	}

	public void setConnAttamptMax(String connAttamptMax) {
		setProperty(CONN_ATTAMPT_MAX, connAttamptMax);
	}

	public String getConnReconnAttamptMax() {
		return getPropertyAsString(CONN_RECONN_ATTAMPT_MAX, DEFAULT_CONN_RECONN_ATTAMPT_MAX);
	}

	public void setConnReconnAttamptMax(String connReconnAttamptMax) {
		setProperty(CONN_RECONN_ATTAMPT_MAX, connReconnAttamptMax);
	}

    public String getUserNameProperty() {
	    return getPropertyAsString(USER_NAME_AUTH, "").trim();
    }

    private String userName;

    public String getUserNameAuth() {
        if (userName == null) {
            String prefix = getUserNameProperty();
            if (isUserNamePrefix()) {
                userName = Util.generateClientId(prefix);
            } else {
                userName = prefix;
            }
        }
        return userName;
    }


	public void setUserNameAuth(String userName) {
		setProperty(USER_NAME_AUTH, userName);
	}

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isUserNamePrefix() {
        return getPropertyAsBoolean(USER_NAME_PREFIX);
    }

	public void setUserNamePrefix(boolean prefix) {
        setProperty(USER_NAME_PREFIX, prefix);
    }
	
	public boolean isKeepTimeShow() {
		return false;
	}
	
	public boolean isConnectionShareShow() {
		return false;
	}

	private String clientId;

	protected String getClientId() {
		if (clientId == null) {
			if (isClientIdSuffix()) {
				clientId = Util.generateClientId(getConnPrefix());
			} else {
				clientId = getConnPrefix();
			}
		}
		return clientId;
	}

	protected String mqttServerIp;
	protected long mqttServerPort;
    private static byte[] commonSecret= {0x00,0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x78,0x79,0x7A,0x7B,0x7C,0x7D,0x7E,0x7F};
    protected String privateSecret;
    protected String token;

    protected boolean getToken(String userId, String clientId) {
        String url = "http://" + getServer() + ":18080/admin/user/token";
        String adminSecret = "123456";
        HttpPost post = null;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            int nonce = (int)(Math.random() * 100000 + 3);
            long timestamp = System.currentTimeMillis();
            String str = nonce + "|" + adminSecret + "|" + timestamp;
            String sign = DigestUtils.sha1Hex(str);


            post = new HttpPost(url);
            post.setHeader("Content-type", "application/json; charset=utf-8");
            post.setHeader("Connection", "Keep-Alive");
            post.setHeader("nonce", nonce + "");
            post.setHeader("timestamp", "" + timestamp);
            post.setHeader("sign", sign);

            String jsonStr = new Gson().toJson(new InputGetToken(userId, clientId));
            logger.info("http request content: " +  jsonStr);

            StringEntity entity = new StringEntity(jsonStr, Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);

            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode != HttpStatus.SC_OK){
                logger.info("Request error: " + statusCode);
                throw new Exception("Http request error with code:" + statusCode);
            }else{
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent(),"utf-8"));
                StringBuffer sb = new StringBuffer();
                String line;
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }

                in.close();

                String content = sb.toString();
                logger.info("http request response content: " + content);

                IMResult<OutputGetIMTokenData> result = fromJsonObject(content, OutputGetIMTokenData.class);
                if (result == null || result.getCode() != IMResult.IMResultCode.IMRESULT_CODE_SUCCESS.code) {
                    logger.error("get token result failure");
                    return false;
                }

                OutputGetIMTokenData out = result.getResult();
                token = out.getToken();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(post != null){
                post.releaseConnection();
            }
        }
        return false;
    }

    private static <T> IMResult<T> fromJsonObject(String content, Class<T> clazz) {
        Type type = ikidou.reflect.TypeBuilder
            .newInstance(IMResult.class)
            .addTypeParam(clazz)
            .build();
        return new Gson().fromJson(content, type);
    }
}
