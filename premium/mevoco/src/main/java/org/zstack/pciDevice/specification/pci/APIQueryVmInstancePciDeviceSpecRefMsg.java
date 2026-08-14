package org.zstack.pciDevice.specification.pci;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
@AutoQuery(replyClass = APIQueryVmInstancePciDeviceSpecRefReply.class, inventoryClass = VmInstancePciDeviceSpecRefInventory.class)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/pci-device-specs",
        optionalPaths = {"/vm-instances/{vmInstanceUuid}/pci-device-specs/{pciSpecUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmInstancePciDeviceSpecRefReply.class
)
public class APIQueryVmInstancePciDeviceSpecRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("vmInstanceUuid=" + uuid());
    }
}
