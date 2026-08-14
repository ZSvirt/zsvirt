package org.zstack.storage.backup.imagestore;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class CleanImageStoreLocalCacheMsg extends NeedReplyMessage implements HostMessage {
    private String mountPath;
    private String hostUuid;

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
