package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryL2VirtualSwitchNetworkReply.class, inventoryClass = L2VirtualSwitchNetworkInventory.class)
@RestRequest(
        path = "/l2-networks/virtual-switch",
        optionalPaths = {"/l2-networks/virtual-switch/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2VirtualSwitchNetworkReply.class
)
public class APIQueryL2VirtualSwitchNetworkMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
