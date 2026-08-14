package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api delete a volume snapshot from primary storage and all its copies from backup stroage
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg": {
 * "uuid": "a4efe4e48c424d009cffe2faae018c70",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "d7d1fb6650d64b20b2b050b0eb06448b"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg": {
 * "uuid": "a4efe4e48c424d009cffe2faae018c70",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "d7d1fb6650d64b20b2b050b0eb06448b"
 * },
 * "timeout": 1800000,
 * "id": "eae63912be604948a2fc19fe1ff907a6",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteVolumeSnapshotEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/volume-snapshots/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVolumeSnapshotEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 6)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "SnapshotUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIDeleteVolumeSnapshotMsg extends APIDeleteMessage implements DeleteVolumeSnapshotMessage {
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(resourceType = VolumeSnapshotVO.class, successIfResourceNotExisting = true)
    private String uuid;

    /**
     * @ignore
     */
    @APINoSee
    private String volumeUuid;
    /**
     * @ignore
     */
    @APINoSee
    private String treeUuid;

    @APIParam(required = false, validValues = {"pull", "commit", "auto"})
    private String direction = "auto";

    @APIParam(required = false, validValues = {"single", "chain", "auto"})
    private String scope = "chain";

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSnapshotUuid() {
        return uuid;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }
 
    public static APIDeleteVolumeSnapshotMsg __example__() {
        APIDeleteVolumeSnapshotMsg msg = new APIDeleteVolumeSnapshotMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
