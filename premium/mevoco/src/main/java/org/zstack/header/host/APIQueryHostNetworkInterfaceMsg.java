package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/24/20.
 */
@AutoQuery(replyClass = APIQueryHostNetworkInterfaceReply.class, inventoryClass = HostNetworkInterfaceInventory.class)
@RestRequest(
        path = "/hosts/nics",
        optionalPaths = {"/hosts/nics/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostNetworkInterfaceReply.class
)
public class APIQueryHostNetworkInterfaceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }
}
