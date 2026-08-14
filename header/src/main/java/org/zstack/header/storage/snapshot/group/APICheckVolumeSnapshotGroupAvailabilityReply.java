package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/12.
 */
@RestResponse(allTo = "results")
public class APICheckVolumeSnapshotGroupAvailabilityReply extends APIReply {
    private List<VolumeSnapshotGroupAvailability> results;

    public List<VolumeSnapshotGroupAvailability> getResults() {
        return results;
    }

    public void setResults(List<VolumeSnapshotGroupAvailability> results) {
        this.results = results;
    }

    public static APICheckVolumeSnapshotGroupAvailabilityReply __example__() {
        APICheckVolumeSnapshotGroupAvailabilityReply reply = new APICheckVolumeSnapshotGroupAvailabilityReply();
        VolumeSnapshotGroupAvailability result = new VolumeSnapshotGroupAvailability(uuid(), "");
        reply.setResults(Collections.singletonList(result));
        return reply;
    }
}
