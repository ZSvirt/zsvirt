package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeBackingChainFromPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private List<String> rootInstallPaths = new ArrayList<>();
    private String primaryStorageUuid;
    // TODO remove it
    private String hostUuid;
    private String volumeFormat;

    public List<String> getRootInstallPaths() {
        return rootInstallPaths;
    }

    public void setRootInstallPaths(List<String> rootInstallPaths) {
        this.rootInstallPaths = rootInstallPaths;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeFormat() {
        return volumeFormat;
    }

    public void setVolumeFormat(String volumeFormat) {
        this.volumeFormat = volumeFormat;
    }
}
