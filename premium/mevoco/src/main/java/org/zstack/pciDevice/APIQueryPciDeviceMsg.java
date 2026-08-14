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
@AutoQuery(replyClass = APIQueryPciDeviceReply.class, inventoryClass = PciDeviceInventory.class)
@RestRequest(
        path = "/pci-device/pci-devices",
        optionalPaths = {"/pci-device/pci-devices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPciDeviceReply.class
)
public class APIQueryPciDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
