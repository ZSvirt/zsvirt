package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 * Created by weiwang on 02/09/2017
 */
@RestRequest(
        path = "/pci-device/candidate-pci-devices-for-new-create-vm",
        method = HttpMethod.GET,
        responseClass = APIGetPciDeviceCandidatesForNewCreateVmReply.class
)
public class APIGetPciDeviceCandidatesForNewCreateVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    @APIParam(required = false)
    private List<String> clusterUuids;

    @APIParam(required = false)
    private List<String> types;

    public static APIGetPciDeviceCandidatesForNewCreateVmMsg __example__() {
        APIGetPciDeviceCandidatesForNewCreateVmMsg msg = new APIGetPciDeviceCandidatesForNewCreateVmMsg();
        msg.setHostUuid(uuid());
        return msg;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }
}
