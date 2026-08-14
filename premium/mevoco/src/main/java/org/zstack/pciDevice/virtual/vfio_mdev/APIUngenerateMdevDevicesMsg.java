package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesEvent;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesMsg;

import java.util.List;

/**
 * Created by GuoYi on 2019-04-20.
 */
@RestRequest(
        path = "/pci-devices/{pciDeviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUngenerateVirtualPciDevicesEvent.class,
        isAction = true
)
public class APIUngenerateMdevDevicesMsg extends APIUngenerateVirtualPciDevicesMsg {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    @APINoSee
    private List<String> mdevUuids;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public List<String> getMdevUuids() {
        return mdevUuids;
    }

    public void setMdevUuids(List<String> mdevUuids) {
        this.mdevUuids = mdevUuids;
    }

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_VFIO_MDEV;
    }

    public static APIUngenerateMdevDevicesMsg __example__() {
        APIUngenerateMdevDevicesMsg msg = new APIUngenerateMdevDevicesMsg();
        msg.setPciDeviceUuid(uuid());
        return msg;
    }
}
