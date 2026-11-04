package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/volume-snapshot-lan-free-layouts",
        method = HttpMethod.POST,
        responseClass = APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply.class,
        parameterName = "params"
)
public class APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg extends APISyncCallMessage {
    @APIParam(nonempty = true, nullElements = false, emptyString = false,
            resourceType = VolumeSnapshotVO.class)
    private List<String> snapshotUuids;

    public List<String> getSnapshotUuids() {
        return snapshotUuids;
    }

    public void setSnapshotUuids(List<String> snapshotUuids) {
        this.snapshotUuids = snapshotUuids;
    }

    public static APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg __example__() {
        APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg msg =
                new APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg();
        msg.setSnapshotUuids(Collections.singletonList(uuid()));
        return msg;
    }
}
