package org.zstack.header.storage.snapshot.group;

/**
 * Created by MaJin on 2019/7/9.
 */

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volume-snapshots/group/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRevertVmFromSnapshotGroupEvent.class
)

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "SnapshotGroupUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIRevertVmFromSnapshotGroupMsg extends APIMessage implements VolumeSnapshotGroupMessage, APIAuditor {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class)
    private String uuid;

    @APINoSee
    private String vmInstanceUuid;

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_CREATION;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIRevertVmFromSnapshotGroupMsg __example__() {
        APIRevertVmFromSnapshotGroupMsg result = new APIRevertVmFromSnapshotGroupMsg();
        result.uuid = uuid();
        return result;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        APIRevertVmFromSnapshotGroupEvent evt = (APIRevertVmFromSnapshotGroupEvent) rsp;
        if (!evt.getResults().stream().allMatch(RevertSnapshotGroupResult::isSuccess)) {
            return null;
        }

        if (((APIRevertVmFromSnapshotGroupMsg) msg).getVmInstanceUuid() == null) {
            return null;
        }

        return new Result(((APIRevertVmFromSnapshotGroupMsg) msg).getVmInstanceUuid(), VmInstanceVO.class);
    }
}
