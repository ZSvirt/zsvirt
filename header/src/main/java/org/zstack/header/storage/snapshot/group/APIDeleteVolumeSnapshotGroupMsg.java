package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestRequest(
        path = "/volume-snapshots/group/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVolumeSnapshotGroupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "SnapshotGroupUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIDeleteVolumeSnapshotGroupMsg extends APIDeleteMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APIParam(required = false, validValues = {"pull", "commit", "auto"})
    private String direction = "auto";

    @APIParam(required = false, validValues = {"single", "chain", "auto"})
    private String scope = "chain";

    @APINoSee
    private String vmUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_DELETION;
    }

    public static APIDeleteVolumeSnapshotGroupMsg __example__() {
        APIDeleteVolumeSnapshotGroupMsg result = new APIDeleteVolumeSnapshotGroupMsg();
        result.uuid = uuid();
        return result;
    }
}
