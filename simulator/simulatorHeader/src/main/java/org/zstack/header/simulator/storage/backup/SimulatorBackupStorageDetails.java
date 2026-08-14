package org.zstack.header.simulator.storage.backup;

public class SimulatorBackupStorageDetails {
	private String url;
	private long totalCapacity;
	private long usedCapacity;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getTotalCapacity() {
		return totalCapacity;
	}

	public void setTotalCapacity(long totalCapacity) {
		this.totalCapacity = totalCapacity;
	}

	public long getUsedCapacity() {
		return usedCapacity;
	}

	public void setUsedCapacity(long usedCapacity) {
		this.usedCapacity = usedCapacity;
	}
}
