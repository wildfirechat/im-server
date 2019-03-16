package net.xmeter;

import java.util.ArrayList;
import java.util.List;

public class SubBean {
	private int receivedMessageSize = 0;
	private int receivedCount = 0;
	private double avgElapsedTime = 0f;

	private List<String> contents = new ArrayList<String>();

	public int getReceivedMessageSize() {
		return receivedMessageSize;
	}

	public void setReceivedMessageSize(int receivedMessageSize) {
		this.receivedMessageSize = receivedMessageSize;
	}

	public int getReceivedCount() {
		return receivedCount;
	}

	public void setReceivedCount(int receivedCount) {
		this.receivedCount = receivedCount;
	}

	public double getAvgElapsedTime() {
		return avgElapsedTime;
	}

	public void setAvgElapsedTime(double avgElapsedTime) {
		this.avgElapsedTime = avgElapsedTime;
	}

	public List<String> getContents() {
		return contents;
	}

	public void setContents(List<String> contents) {
		this.contents = contents;
	}
}
