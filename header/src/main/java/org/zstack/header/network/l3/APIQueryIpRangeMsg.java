package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryIpRangeReply.class, inventoryClass = IpRangeInventory.class)
@RestRequest(
        path = "/l3-networks/ip-ranges",
        optionalPaths = {"/l3-networks/ip-ranges/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIpRangeReply.class
)
public class APIQueryIpRangeMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }

}
