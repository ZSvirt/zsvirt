package org.zstack.pciDevice.specification.pci;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestRequest(
        path = "/pci-device-specs/{pciSpecUuid}/vm-instances/{vmInstanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemovePciDeviceSpecFromVmInstanceEvent.class
)
public class APIRemovePciDeviceSpecFromVmInstanceMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceSpecVO.class)
    private String pciSpecUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getPciSpecUuid() {
        return pciSpecUuid;
    }

    public void setPciSpecUuid(String pciSpecUuid) {
        this.pciSpecUuid = pciSpecUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIRemovePciDeviceSpecFromVmInstanceMsg __example__() {
        APIRemovePciDeviceSpecFromVmInstanceMsg msg = new APIRemovePciDeviceSpecFromVmInstanceMsg();
        msg.setPciSpecUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
