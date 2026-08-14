package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestRequest(
        path = "/volume-snapshots/ungroup/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIUngroupVolumeSnapshotGroupEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "SnapshotGroupUuidToVmUuidResolver", field = "uuid")
public class APIUngroupVolumeSnapshotGroupMsg extends APIMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.NONE;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIUngroupVolumeSnapshotGroupMsg __example__() {
        APIUngroupVolumeSnapshotGroupMsg result = new APIUngroupVolumeSnapshotGroupMsg();
        result.uuid = uuid();
        return result;
    }
}
