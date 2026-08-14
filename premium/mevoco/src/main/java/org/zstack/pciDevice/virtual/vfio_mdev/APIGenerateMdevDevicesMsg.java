package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesEvent;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesMsg;

/**
 * Created by GuoYi on 2019-04-18.
 */
@RestRequest(
        path = "/pci-devices/{pciDeviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIGenerateVirtualPciDevicesEvent.class,
        isAction = true
)
public class APIGenerateMdevDevicesMsg extends APIGenerateVirtualPciDevicesMsg {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    @APIParam(resourceType = MdevDeviceSpecVO.class)
    private String mdevSpecUuid;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_VFIO_MDEV;
    }

    public static APIGenerateMdevDevicesMsg __example__() {
        APIGenerateMdevDevicesMsg msg = new APIGenerateMdevDevicesMsg();
        msg.setPciDeviceUuid(uuid());
        msg.setMdevSpecUuid(uuid());
        return msg;
    }
}
