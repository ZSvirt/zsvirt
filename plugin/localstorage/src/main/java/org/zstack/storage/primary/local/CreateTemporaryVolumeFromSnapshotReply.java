package org.zstack.storage.primary.local;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/29.
 */
public class CreateTemporaryVolumeFromSnapshotReply extends MessageReply {
    private String installPath;
    private long size;
    private long actualSize;
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
