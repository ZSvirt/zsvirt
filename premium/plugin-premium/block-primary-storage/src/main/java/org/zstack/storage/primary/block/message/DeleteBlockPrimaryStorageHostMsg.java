package org.zstack.storage.primary.block.message;

import org.zstack.header.message.DeletionMessage;

import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/7/19 16:31
 */
public class DeleteBlockPrimaryStorageHostMsg extends DeletionMessage {
    private String hostUuid;

    private String primaryStorageUuid;
    private String metadata;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
