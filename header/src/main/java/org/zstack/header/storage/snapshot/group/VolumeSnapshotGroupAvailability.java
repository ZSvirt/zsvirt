package org.zstack.header.storage.snapshot.group;

import org.apache.commons.lang.StringUtils;

/**
 * Created by MaJin on 2019/7/12.
 */
public class VolumeSnapshotGroupAvailability {
    private String uuid;
    private boolean available;
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public VolumeSnapshotGroupAvailability() {

    }

    public VolumeSnapshotGroupAvailability(String uuid, String reason) {
        this.uuid = uuid;
        this.available = StringUtils.isEmpty(reason);
        this.reason = reason;
    }
}
