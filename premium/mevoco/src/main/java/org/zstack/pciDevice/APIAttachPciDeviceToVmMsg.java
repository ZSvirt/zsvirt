package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestRequest(
        path = "/pci-device/pci-devices/{pciDeviceUuid}/attach",
        method = HttpMethod.POST,
        responseClass = APIAttachPciDeviceToVmEvent.class,
        parameterName = "params"
)
public class APIAttachPciDeviceToVmMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public static APIAttachPciDeviceToVmMsg __example__() {
        APIAttachPciDeviceToVmMsg msg = new APIAttachPciDeviceToVmMsg();
        msg.setPciDeviceUuid(uuid());
        msg.setVmInstanceUuid(uuid());

        return msg;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
