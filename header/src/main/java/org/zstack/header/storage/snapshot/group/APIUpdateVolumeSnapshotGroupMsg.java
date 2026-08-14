package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

/**
 * Created by MaJin on 2019/8/29.
 */
@RestRequest(
        path = "/volume-snapshots/group/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVolumeSnapshotGroupEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "SnapshotGroupUuidToVmUuidResolver", field = "uuid")
public class APIUpdateVolumeSnapshotGroupMsg extends APIMessage implements VolumeSnapshotGroupMessage {
    @APIParam(required = false)
    private String name;

    @APIParam(required = false)
    private String description;

    @APIParam(resourceType = VolumeSnapshotGroupVO.class)
    private String uuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.NONE;
    }

    public static APIUpdateVolumeSnapshotGroupMsg __example__() {
        APIUpdateVolumeSnapshotGroupMsg msg = new APIUpdateVolumeSnapshotGroupMsg();
        msg.uuid = uuid();
        msg.name = "new name";
        return msg;
    }
}
