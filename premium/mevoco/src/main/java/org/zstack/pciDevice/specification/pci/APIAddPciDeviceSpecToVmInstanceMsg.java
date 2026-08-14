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
        method = HttpMethod.POST,
        responseClass = APIAddPciDeviceSpecToVmInstanceEvent.class,
        parameterName = "params"
)
public class APIAddPciDeviceSpecToVmInstanceMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceSpecVO.class)
    private String pciSpecUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    // assume there are less than 100 devices releated to one spec in one host
    @APIParam(required = false, numberRange = {1, 100})
    private Integer pciDeviceNumber = 1;

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

    public Integer getPciDeviceNumber() {
        return pciDeviceNumber;
    }

    public void setPciDeviceNumber(Integer pciDeviceNumber) {
        this.pciDeviceNumber = pciDeviceNumber;
    }

    public static APIAddPciDeviceSpecToVmInstanceMsg __example__() {
        APIAddPciDeviceSpecToVmInstanceMsg msg = new APIAddPciDeviceSpecToVmInstanceMsg();
        msg.setPciSpecUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
