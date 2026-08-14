package org.zstack.pciDevice.virtual.sr_iov;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.pciDevice.PciDeviceConstants;
import org.zstack.pciDevice.PciDeviceVO;
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
public class APIGenerateSriovPciDevicesMsg extends APIGenerateVirtualPciDevicesMsg {
    @APIParam(resourceType = PciDeviceVO.class)
    private String pciDeviceUuid;

    @APIParam
    private Integer virtPartNum;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public Integer getVirtPartNum() {
        return virtPartNum;
    }

    public void setVirtPartNum(Integer virtPartNum) {
        this.virtPartNum = virtPartNum;
    }

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_SR_IOV;
    }

    public static APIGenerateSriovPciDevicesMsg __example__() {
        APIGenerateSriovPciDevicesMsg msg = new APIGenerateSriovPciDevicesMsg();
        msg.setPciDeviceUuid(uuid());
        msg.setVirtPartNum(4);
        return msg;
    }
}
