package org.zstack.header.baremetal.network;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-01-03.
 */
@AutoQuery(replyClass = APIQueryBaremetalBondingReply.class, inventoryClass = BaremetalBondingInventory.class)
@RestRequest(
        path = "/baremetal/network/bondings",
        optionalPaths = {"/baremetal/network/bondings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBaremetalBondingReply.class
)
public class APIQueryBaremetalBondingMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
