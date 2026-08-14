package org.zstack.header.storage.snapshot;

import java.io.Serializable;

/**
 * Create by weiwang at 2018/6/11
 */
public class TakeSnapshotsOnKvmResultStruct implements Serializable {
    private String volumeUuid;
    private long size;
    private String installPath;
    private String previousInstallPath;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getPreviousInstallPath() {
        return previousInstallPath;
    }

    public void setPreviousInstallPath(String previousInstallPath) {
        this.previousInstallPath = previousInstallPath;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
