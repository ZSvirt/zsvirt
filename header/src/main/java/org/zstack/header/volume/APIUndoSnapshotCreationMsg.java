package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * @ Author : yh.w
 * @ Date   : Created in 15:40 2023/8/21
 */
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUndoSnapshotCreationEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIUndoSnapshotCreationMsg extends APIMessage implements VolumeMessage {

    @APIParam(resourceType = VolumeVO.class)
    private String uuid;
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String snapShotUuid;

    public String getSnapShotUuid() {
        return snapShotUuid;
    }

    public void setSnapShotUuid(String snapShotUuid) {
        this.snapShotUuid = snapShotUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public static APIUndoSnapshotCreationMsg __example__() {
        APIUndoSnapshotCreationMsg msg = new APIUndoSnapshotCreationMsg();
        msg.setUuid(uuid());
        msg.setSnapShotUuid(uuid());
        return msg;
    }
}
