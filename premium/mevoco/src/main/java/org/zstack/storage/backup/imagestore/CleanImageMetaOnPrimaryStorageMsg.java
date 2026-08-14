package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:53 2023/10/18
 */
public class CleanImageMetaOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageInstallPath;
    private String psUuid;
    private String volumeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public String getPsUuid() {
        return psUuid;
    }

    public void setPsUuid(String psUuid) {
        this.psUuid = psUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return psUuid;
    }
}
