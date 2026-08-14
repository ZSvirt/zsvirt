package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/volume-snapshots/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVolumeSnapshotEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "SnapshotUuidToVmUuidResolver", field = "uuid")
public class APIUpdateVolumeSnapshotMsg extends APIMessage implements VolumeSnapshotMessage {
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APINoSee
    private String volumeUuid;
    @APINoSee
    private String treeUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    @Override
    public String getSnapshotUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }
 
    public static APIUpdateVolumeSnapshotMsg __example__() {
        APIUpdateVolumeSnapshotMsg msg = new APIUpdateVolumeSnapshotMsg();

        msg.setUuid(uuid());
        msg.setName("My Snapshot");

        return msg;
    }
}
