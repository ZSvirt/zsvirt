package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 17/06/2017.
 */
@AutoQuery(replyClass = APIQueryPciDevicePciDeviceOfferingReply.class, inventoryClass = PciDevicePciDeviceOfferingRefInventory.class)
@RestRequest(
        path = "/pci-devices/pci-devices/pci-device-offerings",
        method = HttpMethod.GET,
        responseClass = APIQueryPciDevicePciDeviceOfferingReply.class
)
public class APIQueryPciDevicePciDeviceOfferingMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
