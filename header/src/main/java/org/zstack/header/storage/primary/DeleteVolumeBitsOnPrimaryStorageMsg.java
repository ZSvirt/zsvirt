package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.ReplayableMessage;

/**
 * DeleteVolumeBitsOnPrimaryStorageMsg means we delete volume
 * @see DeleteBitsOnPrimaryStorageMsg
 */
public class DeleteVolumeBitsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage, ReplayableMessage {
    private String primaryStorageUuid;
    private String installPath;
    private String hypervisorType;
    private boolean folder;
    private String bitsUuid;
    private String bitsType;
    private String hostUuid;
    // used for recycle, true means only delete install path, not delete volume
    private boolean fromRecycle;
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getBitsUuid() {
        return bitsUuid;
    }

    public void setBitsUuid(String bitsUuid) {
        this.bitsUuid = bitsUuid;
    }

    public String getBitsType() {
        return bitsType;
    }

    public void setBitsType(String bitsType) {
        this.bitsType = bitsType;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isFromRecycle() {
        return fromRecycle;
    }

    public void setFromRecycle(boolean fromRecycle) {
        this.fromRecycle = fromRecycle;
    }

    @Override
    public String getResourceUuid() {
        return bitsUuid;
    }

    @Override
    public Class getReplayableClass() {
        return DeleteVolumeBitsOnPrimaryStorageMsg.class;
    }
}
