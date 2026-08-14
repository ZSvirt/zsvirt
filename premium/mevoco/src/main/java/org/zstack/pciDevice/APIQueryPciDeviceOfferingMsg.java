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
@AutoQuery(replyClass = APIQueryPciDeviceOfferingReply.class, inventoryClass = PciDeviceOfferingInventory.class)
@RestRequest(
        path = "/pci-device/pci-device-offerings",
        optionalPaths = {"/pci-device/pci-device-offerings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPciDeviceOfferingReply.class
)
public class APIQueryPciDeviceOfferingMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
