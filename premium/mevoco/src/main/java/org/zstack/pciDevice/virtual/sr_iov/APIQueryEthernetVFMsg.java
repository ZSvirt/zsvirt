package org.zstack.pciDevice.virtual.sr_iov;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.sriov.EthernetVfPciDeviceInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by shixin.ruan on 12/19/2023.
 */
@AutoQuery(replyClass = APIQueryEthernetVFReply.class, inventoryClass = EthernetVfPciDeviceInventory.class)
@RestRequest(
        path = "/pci-device/ethernet-vfs",
        optionalPaths = {"/pci-device/ethernet-vfs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryEthernetVFReply.class
)
public class APIQueryEthernetVFMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
