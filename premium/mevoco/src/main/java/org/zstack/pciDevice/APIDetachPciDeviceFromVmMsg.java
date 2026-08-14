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
        path = "/pci-device/pci-devices/{pciDeviceUuid}/detach",
        method = HttpMethod.POST,
        responseClass = APIDetachPciDeviceFromVmEvent.class,
        parameterName = "params"
)
public class APIDetachPciDeviceFromVmMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public static APIDetachPciDeviceFromVmMsg __example__() {
        APIDetachPciDeviceFromVmMsg msg = new APIDetachPciDeviceFromVmMsg();
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
