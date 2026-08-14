package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 15/03/2017.
 */
@AutoQuery(replyClass = APIQueryVniRangeReply.class, inventoryClass = VniRangeInventory.class)
@RestRequest(
        path = "/l2-networks/vxlan-pool/vni-range",
        optionalPaths = {"/l2-networks/vxlan-pool/vni-range/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVniRangeReply.class
)
public class APIQueryVniRangeMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
