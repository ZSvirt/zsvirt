package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"luns", "layouts"})
public class APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply extends APIReply {
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

    public static APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply __example__() {
        APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply reply =
                new APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply();
        reply.setLuns(Collections.singletonList(SharedBlockLunDescriptor.__example__()));
        reply.setLayouts(Collections.singletonList(
                SharedBlockVolumeSnapshotLanFreeLayout.__example__()));
        return reply;
    }
}
