package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

public class GetSharedBlockVolumeSnapshotLanFreeLayoutReply extends MessageReply {
    private List<SharedBlockLunDescriptor> luns = new ArrayList<>();
    private List<SharedBlockVolumeSnapshotLanFreeLayout> layouts = new ArrayList<>();

    public List<SharedBlockLunDescriptor> getLuns() {
        return luns;
    }

    public void setLuns(List<SharedBlockLunDescriptor> luns) {
        this.luns = luns;
    }

    public List<SharedBlockVolumeSnapshotLanFreeLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<SharedBlockVolumeSnapshotLanFreeLayout> layouts) {
        this.layouts = layouts;
    }
}
