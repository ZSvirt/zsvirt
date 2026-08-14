package org.zstack.header.simulator.storage.primary;

public class SimulatorPrimaryStorageDetails {
	private String url;
	private long totalCapacity;
	private long availableCapacity;
	private String zoneUuid;

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

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

	public String getZoneUuid() {
		return zoneUuid;
	}

	public void setZoneUuid(String zoneUuid) {
		this.zoneUuid = zoneUuid;
	}
	
}
