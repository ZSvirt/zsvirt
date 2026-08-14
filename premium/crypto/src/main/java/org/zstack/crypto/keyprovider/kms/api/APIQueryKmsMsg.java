package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryKmsReply.class, inventoryClass = KmsInventory.class)
@RestRequest(
        path = "/key-providers/kms",
        optionalPaths = {"/key-providers/kms/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryKmsReply.class
)
public class APIQueryKmsMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(KmsVO.class));
    }
}
