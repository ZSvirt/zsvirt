package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryIpAddressReply.class, inventoryClass = UsedIpInventory.class)
@RestRequest(
        path = "/l3-networks/ip-address",
        optionalPaths = {"/l3-networks/ip-address/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIpAddressReply.class
)
public class APIQueryIpAddressMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
