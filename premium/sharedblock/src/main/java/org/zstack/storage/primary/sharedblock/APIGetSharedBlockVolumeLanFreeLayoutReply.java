package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"luns", "layouts"})
public class APIGetSharedBlockVolumeLanFreeLayoutReply extends APIReply {
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

    public static APIGetSharedBlockVolumeLanFreeLayoutReply __example__() {
        APIGetSharedBlockVolumeLanFreeLayoutReply reply =
                new APIGetSharedBlockVolumeLanFreeLayoutReply();
        reply.setLuns(Collections.singletonList(SharedBlockLunDescriptor.__example__()));
        reply.setLayouts(Collections.singletonList(
                SharedBlockVolumeLanFreeLayout.__example__()));
        return reply;
    }
}
