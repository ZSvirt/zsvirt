package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/pci-device/pci-devices/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePciDeviceEvent.class
)
@Deprecated
public class APIDeletePciDeviceMsg extends APIDeleteMessage {
    @APIParam(resourceType = PciDeviceVO.class)
    private String uuid;

    public static APIDeletePciDeviceMsg __example__() {
        APIDeletePciDeviceMsg msg = new APIDeletePciDeviceMsg();
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
