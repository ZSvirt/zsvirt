package org.zstack.storage.primary.nfs;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by GuoYi on 10/19/17.
 */
public class NfsToNfsMigrateBitsMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private String srcFolderPath;
    private String dstFolderPath;
    private String srcPrimaryStorageUuid;
    private String primaryStorageUuid;
    private String independentPath;
    private String volumeInstallPath;

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getSrcFolderPath() {
        return srcFolderPath;
    }

    public void setSrcFolderPath(String srcFolderPath) {
        this.srcFolderPath = srcFolderPath;
    }

    public String getDstFolderPath() {
        return dstFolderPath;
    }

    public void setDstFolderPath(String dstFolderPath) {
        this.dstFolderPath = dstFolderPath;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public String getSrcPrimaryStorageUuid() {
        return srcPrimaryStorageUuid;
    }

    public void setSrcPrimaryStorageUuid(String srcPrimaryStorageUuid) {
        this.srcPrimaryStorageUuid = srcPrimaryStorageUuid;
    }

    public String getIndependentPath() {
        return independentPath;
    }

    public void setIndependentPath(String independentPath) {
        this.independentPath = independentPath;
    }
}
