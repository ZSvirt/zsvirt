package org.zstack.crypto.keyprovider.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryKeyProviderReply.class, inventoryClass = KeyProviderInventory.class)
@RestRequest(
        path = "/key-providers",
        optionalPaths = {"/key-providers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryKeyProviderReply.class
)
public class APIQueryKeyProviderMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(KeyProviderVO.class));
    }
}
