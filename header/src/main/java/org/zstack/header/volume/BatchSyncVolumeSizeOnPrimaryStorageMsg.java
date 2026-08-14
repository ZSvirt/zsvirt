package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.Map;

public class BatchSyncVolumeSizeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;

    private String primaryStorageUuid;

    private Map<String, String> volumeUuidInstallPaths;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setVolumeUuidInstallPaths(Map<String, String> volumeUuidInstallPaths) {
        this.volumeUuidInstallPaths = volumeUuidInstallPaths;
    }

    public Map<String, String> getVolumeUuidInstallPaths() {
        return volumeUuidInstallPaths;
    }
}
