package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.MessageReply;

import java.util.List;
import java.util.Map;

public class PingHostAttachedPrimaryStoragesReply extends MessageReply {
    private Map<String, String> disconnectedPriamryStorageUuids;

    public Map<String, String> getDisconnectedPriamryStorageUuids() {
        return disconnectedPriamryStorageUuids;
    }

    public void setDisconnectedPriamryStorageUuids(Map<String, String> disconnectedPriamryStorageUuids) {
        this.disconnectedPriamryStorageUuids = disconnectedPriamryStorageUuids;
    }
}
