package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPortGroupReply.class, inventoryClass = PortGroupInventory.class)
@RestRequest(
        path = "/l3-networks/port-group",
        optionalPaths = {"/l3-networks/port-group/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPortGroupReply.class
)
public class APIQueryPortGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
