package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.CollectionUtils;

import java.util.List;

public class DeleteVolumeChainOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    // TODO remove it
    private String hostUuid;

    private List<String> installPaths;

    private String volumeFormat;

    // for gc, used to deduplicate GC
    private String chainTop;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setInstallPaths(List<String> installPaths) {
        this.installPaths = installPaths;
    }

    public List<String> getInstallPaths() {
        return installPaths;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
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

    public String getChainTop() {
        if (chainTop != null) {
            return chainTop;
        } else if (!CollectionUtils.isEmpty(installPaths)) {
            return installPaths.get(0);
        }
        return null;
    }

    public void setChainTop(String chainTop) {
        this.chainTop = chainTop;
    }
}
