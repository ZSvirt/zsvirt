package org.zstack.pciDevice.virtual.sr_iov;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesEvent;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesMsg;

/**
 * Created by GuoYi on 2019-04-25.
 */
@RestRequest(
        path = "/pci-devices/{pciDeviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUngenerateVirtualPciDevicesEvent.class,
        isAction = true
)
public class APIUngenerateSriovPciDevicesMsg extends APIUngenerateVirtualPciDevicesMsg {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_SR_IOV;
    }

    public static APIUngenerateSriovPciDevicesMsg __example__() {
        APIUngenerateSriovPciDevicesMsg msg = new APIUngenerateSriovPciDevicesMsg();
        msg.setPciDeviceUuid(uuid());
        return msg;
    }
}
