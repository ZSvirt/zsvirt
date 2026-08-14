package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestRequest(
        path = "/pci-device/pci-device-offerings",
        method = HttpMethod.POST,
        responseClass = APICreatePciDeviceOfferingEvent.class,
        parameterName = "params"
)
public class APICreatePciDeviceOfferingMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APINoSee
    @APIParam(required = false)
    private String type;

    @APIParam(maxLength = 4)
    private String vendorId;

    @APIParam(maxLength = 4)
    private String deviceId;

    @APIParam(maxLength = 4, required = false)
    private String subvendorId;

    @APIParam(maxLength = 4, required = false)
    private String subdeviceId;

    @APIParam(required = false)
    private String ramSize;

    public static APICreatePciDeviceOfferingMsg __example__() {
        APICreatePciDeviceOfferingMsg msg = new APICreatePciDeviceOfferingMsg();
        msg.setName("test");
        msg.setDeviceId("0e0f");
        msg.setVendorId("10de");
        return msg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getRamSize() {
        return ramSize;
    }

    public void setRamSize(String ramSize) {
        this.ramSize = ramSize;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreatePciDeviceOfferingEvent)rsp).getInventory().getUuid() : "", PciDeviceOfferingVO.class);
    }
}
