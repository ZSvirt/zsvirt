package org.zstack.pciDevice.specification.pci;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-03-06.
 */
@AutoQuery(replyClass = APIQueryPciDeviceSpecReply.class, inventoryClass = PciDeviceSpecInventory.class)
@RestRequest(
        path = "/pci-device-specs",
        optionalPaths = {"/pci-device-specs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPciDeviceSpecReply.class
)
public class APIQueryPciDeviceSpecMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
