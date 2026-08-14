package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/routerArea/network",
        optionalPaths = {"/routerArea/networkR/{uuid}"},
        responseClass = APIQueryVRouterOspfNetworkReply.class,
        method = HttpMethod.GET
)
@AutoQuery(replyClass = APIQueryVRouterOspfNetworkReply.class, inventoryClass = NetworkRouterAreaRefInventory.class)
public class APIQueryVRouterOspfNetworkMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return list("l3NetworkUuid=" + uuid(L3NetworkVO.class));
    }
}
