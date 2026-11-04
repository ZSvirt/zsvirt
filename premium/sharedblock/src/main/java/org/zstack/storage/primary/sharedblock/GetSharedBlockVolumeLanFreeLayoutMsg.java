package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.ArrayList;
import java.util.List;

public class GetSharedBlockVolumeLanFreeLayoutMsg extends NeedReplyMessage
        implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private List<SharedBlockVolumeLanFreeTarget> targets = new ArrayList<>();

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<SharedBlockVolumeLanFreeTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<SharedBlockVolumeLanFreeTarget> targets) {
        this.targets = targets;
    }
}
