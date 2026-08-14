package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryNkpReply.class, inventoryClass = NkpInventory.class)
@RestRequest(
        path = "/key-providers/nkp",
        optionalPaths = {"/key-providers/nkp/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNkpReply.class
)
public class APIQueryNkpMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(NkpVO.class));
    }
}
