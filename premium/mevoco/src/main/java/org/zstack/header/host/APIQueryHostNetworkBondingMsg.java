package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/24/20.
 */
@AutoQuery(replyClass = APIQueryHostNetworkBondingReply.class, inventoryClass = HostNetworkBondingInventory.class)
@RestRequest(
        path = "/hosts/bondings",
        optionalPaths = {"/hosts/bondings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostNetworkBondingReply.class
)
public class APIQueryHostNetworkBondingMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }
}
