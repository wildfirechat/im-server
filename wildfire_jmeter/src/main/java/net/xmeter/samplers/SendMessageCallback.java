package net.xmeter.samplers;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.fusesource.mqtt.client.Callback;

public class SendMessageCallback implements Callback<byte[]>{
	private static Logger logger = LoggingManager.getLoggerForClass();
	private boolean successful = false;
	private Object connLock;
	private int errorCode;
	
	public SendMessageCallback(Object connLock) {
		this.connLock = connLock;
	}
	
	@Override
	public void onSuccess(byte[] value) {
		//If QoS == 0, then the current thread is the same thread of caller thread.
		//Else if QoS == 1 | 2, then the current thread is hawtdispatch-DEFAULT-x
        if (value != null && value.length > 0) {
            errorCode = value[0]&0xff;
        } else {
            onFailure(new Throwable("Send message with invalid data back"));
            return;
        }
        if (errorCode != 0) {
            onFailure(new Throwable("Send message failure with error code(" + errorCode + ")"));
            return;
        }
		synchronized (connLock) {
			this.successful = true;
			connLock.notify();
		}
	}
	
	@Override
	public void onFailure(Throwable value) {
		synchronized (connLock) {
			this.successful = false;
			logger.log(Priority.ERROR, value.getMessage(), value);
			connLock.notify();
		}
	}

	public boolean isSuccessful() {
		return successful;
	}

    public int getErrorCode() {
        return errorCode;
    }
}
