package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

public class GetSharedBlockVolumeLanFreeLayoutReply extends MessageReply {
    private List<SharedBlockLunDescriptor> luns = new ArrayList<>();
    private List<SharedBlockVolumeLanFreeLayout> layouts = new ArrayList<>();

    public List<SharedBlockLunDescriptor> getLuns() {
        return luns;
    }

    public void setLuns(List<SharedBlockLunDescriptor> luns) {
        this.luns = luns;
    }

    public List<SharedBlockVolumeLanFreeLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<SharedBlockVolumeLanFreeLayout> layouts) {
        this.layouts = layouts;
    }
}
