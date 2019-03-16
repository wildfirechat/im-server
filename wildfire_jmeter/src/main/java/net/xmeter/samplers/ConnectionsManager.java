package net.xmeter.samplers;

import java.util.concurrent.ConcurrentHashMap;

import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;

public class ConnectionsManager {
    public static class ConnectionInfo {
        public String userName;
        public String token;
        public String privateSecrect;
        public String serverAddress;
        public long serverPort;

        public ConnectionInfo(String userName, String token, String privateSecrect, String serverAddress, long serverPort) {
            this.userName = userName;
            this.token = token;
            this.privateSecrect = privateSecrect;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }
    }

	private ConcurrentHashMap<String, CallbackConnection> connections = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Boolean> connectionsStatus = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConnectionInfo> connectionInfos = new ConcurrentHashMap<>();
	
	private static ConnectionsManager connectionsManager = new ConnectionsManager();
	private ConnectionsManager() {
		
	}
	
	public static synchronized ConnectionsManager getInstance() {
		return connectionsManager;
	}
	
	public CallbackConnection createConnection(String key, MQTT mqtt, ConnectionInfo connectionInfo) {
		CallbackConnection conn = mqtt.callbackConnection();
		connections.put(key, conn);
		connectionInfos.put(key, connectionInfo);
		return conn;
	}
	
	public CallbackConnection getConnection(String key) {
		return this.connections.get(key);
	}

    public ConnectionInfo getConnectionInfo(String key) {
        return this.connectionInfos.get(key);
    }
	
	public boolean containsConnection(String key) {
		return connections.containsKey(key);
	}
	
	public void removeConnection(String key) {
		this.connections.remove(key);
		this.connectionInfos.remove(key);
	}
	
	public void setConnectionStatus(String key, Boolean status) {
		connectionsStatus.put(key, status);
	}
	
	public boolean getConnectionStatus(String key) {
		if(!connectionsStatus.containsKey(key)) {
			throw new RuntimeException("Cannot find conn status for key: " + key);
		}
		return connectionsStatus.get(key);
	}
}
