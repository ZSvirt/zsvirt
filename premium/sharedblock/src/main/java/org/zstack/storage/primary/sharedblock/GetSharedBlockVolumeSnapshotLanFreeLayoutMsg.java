package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.ArrayList;
import java.util.List;

public class GetSharedBlockVolumeSnapshotLanFreeLayoutMsg extends NeedReplyMessage
        implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private List<SharedBlockVolumeSnapshotLanFreeTarget> targets = new ArrayList<>();

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<SharedBlockVolumeSnapshotLanFreeTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<SharedBlockVolumeSnapshotLanFreeTarget> targets) {
        this.targets = targets;
    }
}
