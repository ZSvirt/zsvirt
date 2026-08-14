package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by MaJin on 2019/6/4.
 */
public class GetPrimaryStorageResourceLocationReply extends MessageReply {
    private List<String> hostUuids;
    private String primaryStorageUuid;
    private String installPath;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
