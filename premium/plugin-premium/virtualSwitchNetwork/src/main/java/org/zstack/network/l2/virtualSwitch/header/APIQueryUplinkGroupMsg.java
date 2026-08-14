package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryUplinkGroupReply.class, inventoryClass = UplinkGroupInventory.class)
@RestRequest(
        path = "/l2-networks/virtual-switch/uplink-group",
        optionalPaths = {"/l2-networks/virtual-switch/uplink-group/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryUplinkGroupReply.class
)
public class APIQueryUplinkGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
