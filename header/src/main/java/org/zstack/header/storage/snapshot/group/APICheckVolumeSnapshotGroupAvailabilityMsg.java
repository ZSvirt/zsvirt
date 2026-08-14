package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/12.
 */
@RestRequest(
        path = "/volume-snapshots/groups/availabilities",
        method = HttpMethod.GET,
        responseClass = APICheckVolumeSnapshotGroupAvailabilityReply.class
)
public class APICheckVolumeSnapshotGroupAvailabilityMsg extends APISyncCallMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APICheckVolumeSnapshotGroupAvailabilityMsg __example__() {
        APICheckVolumeSnapshotGroupAvailabilityMsg result = new APICheckVolumeSnapshotGroupAvailabilityMsg();
        result.uuids = Collections.singletonList(uuid());
        return result;
    }
}
