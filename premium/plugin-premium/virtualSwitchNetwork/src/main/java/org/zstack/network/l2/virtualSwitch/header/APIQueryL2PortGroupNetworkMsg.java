package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@Deprecated
@AutoQuery(replyClass = APIQueryL2PortGroupNetworkReply.class, inventoryClass = L2PortGroupNetworkInventory.class)
@RestRequest(
        path = "/l2-networks/port-group",
        optionalPaths = {"/l2-networks/port-group/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2PortGroupNetworkReply.class
)
public class APIQueryL2PortGroupNetworkMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
