package org.zstack.pciDevice.specification.pci;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.pciDevice.PciDeviceType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 2019-03-13.
 */
@RestRequest(
        path = "/pci-device-specs/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetPciDeviceSpecCandidatesReply.class
)
public class APIGetPciDeviceSpecCandidatesMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private List<String> clusterUuids;

    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    // get pci spec candidates for a specific vm instance
    @APIParam(resourceType = VmInstanceVO.class, required = false)
    private String vmInstanceUuid;

    // get pci spec candidates for a batch of vm instances
    @APIParam(resourceType = VmInstanceVO.class, required = false)
    private List<String> vmInstanceUuids;

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

    public List<String> getVmInstanceUuids() {
        return vmInstanceUuids;
    }

    public void setVmInstanceUuids(List<String> vmInstanceUuids) {
        this.vmInstanceUuids = vmInstanceUuids;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public static APIGetPciDeviceSpecCandidatesMsg __example__() {
        APIGetPciDeviceSpecCandidatesMsg msg = new APIGetPciDeviceSpecCandidatesMsg();
        msg.setClusterUuids(Arrays.asList(uuid(), uuid()));
        msg.setHostUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        msg.setTypes(Arrays.asList(
                PciDeviceType.GPU_Video_Controller.toString(),
                PciDeviceType.GPU_Audio_Controller.toString()
        ));
        return msg;
    }
}
