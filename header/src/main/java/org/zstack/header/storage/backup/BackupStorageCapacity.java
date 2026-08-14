package org.zstack.header.storage.backup;

/**
 * Created by xing5 on 2016/4/28.
 */
public class BackupStorageCapacity {
    private String uuid;
    private long totalCapacity;
    private long availableCapacity;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
}
