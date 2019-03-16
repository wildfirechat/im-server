package net.xmeter.samplers;

import java.text.MessageFormat;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;

public class ConnectionCallback implements Callback<byte[]>{
	private static Logger logger = LoggingManager.getLoggerForClass();
	private Object connLock;
	private CallbackConnection connection;
	private boolean connectionSucc = false;
	
	public ConnectionCallback(CallbackConnection connection, Object connLock) {
		this.connection = connection;
		this.connLock = connLock;
	}
	@Override
	public void onSuccess(byte[] value) {
		synchronized (connLock) {
			logger.info(MessageFormat.format("The connection {0} is established successfully.", this.connection + Thread.currentThread().getName()));
			connectionSucc = true;
			this.connLock.notify();	
		}
	}

	@Override
	public void onFailure(Throwable value) {
		synchronized (connLock) {
			connectionSucc = false;
			logger.log(Priority.ERROR, value.getMessage(), value);
			this.connLock.notify();			
		}
	}
	
	public boolean isConnectionSucc() {
		return connectionSucc;
	}
}
