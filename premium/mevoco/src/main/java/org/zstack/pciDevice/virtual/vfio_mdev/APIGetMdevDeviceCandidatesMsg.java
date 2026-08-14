package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-05.
 */
@RestRequest(
        path = "/mdev-devices/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetMdevDeviceCandidatesReply.class
)
public class APIGetMdevDeviceCandidatesMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private List<String> clusterUuids;

    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    @APIParam(resourceType = VmInstanceVO.class, required = false)
    private String vmInstanceUuid;

    @APIParam(required = false)
    private List<String> types;

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public static APIGetMdevDeviceCandidatesMsg __example__() {
        APIGetMdevDeviceCandidatesMsg msg = new APIGetMdevDeviceCandidatesMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setTypes(Collections.singletonList(MdevDeviceType.GPU_Video_Controller.toString()));
        return msg;
    }
}
