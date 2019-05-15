package net.xmeter.samplers;

import java.util.concurrent.ConcurrentHashMap;

import cn.wildfirechat.client.IMClient;

public class ClientManager {
    private ConcurrentHashMap<String, IMClient> clients = new ConcurrentHashMap<>();

	
	private static ClientManager clientManager = new ClientManager();
	private ClientManager() {
		
	}
	
	public static synchronized ClientManager getInstance() {
		return clientManager;
	}
	
	public void putClient(String key, IMClient client) {
		clients.put(key, client);
	}
	
	public IMClient getClient(String key) {
		return this.clients.get(key);
	}


	public void removeClient(String key) {
		this.clients.remove(key);
	}
}
