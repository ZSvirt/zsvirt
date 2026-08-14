package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;

import java.util.List;

/**
 * Created by weiwang on 02/09/2017
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/candidate-pci-devices",
        method = HttpMethod.GET,
        responseClass = APIGetPciDeviceCandidatesForAttachingVmReply.class
)
public class APIGetPciDeviceCandidatesForAttachingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false)
    private List<String> types;

    @APIParam(resourceType = PciDeviceSpecVO.class, required = false)
    private List<String> pciSpecUuids;

    public static APIAttachPciDeviceToVmMsg __example__() {
        APIAttachPciDeviceToVmMsg msg = new APIAttachPciDeviceToVmMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setPciDeviceUuid(uuid());
        return msg;
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

    public List<String> getPciSpecUuids() {
        return pciSpecUuids;
    }

    public void setPciSpecUuids(List<String> pciSpecUuids) {
        this.pciSpecUuids = pciSpecUuids;
    }
}
