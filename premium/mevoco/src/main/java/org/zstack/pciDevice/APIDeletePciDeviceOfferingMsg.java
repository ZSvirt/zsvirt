package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestRequest(
        path = "/pci-device/pci-device-offerings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePciDeviceOfferingEvent.class
)
public class APIDeletePciDeviceOfferingMsg extends APIDeleteMessage {
    @APIParam(resourceType = PciDeviceOfferingVO.class)
    private String uuid;

    public static APIDeletePciDeviceOfferingMsg __example__() {
        APIDeletePciDeviceOfferingMsg msg = new APIDeletePciDeviceOfferingMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
