package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

@RestRequest(
        path = "/volume-snapshots/group/trees",
        method = HttpMethod.GET,
        responseClass = APIGetVolumeSnapshotGroupTreeReply.class
)
public class APIGetVolumeSnapshotGroupTreeMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIGetVolumeSnapshotGroupTreeMsg __example__() {
        APIGetVolumeSnapshotGroupTreeMsg msg = new APIGetVolumeSnapshotGroupTreeMsg();
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
