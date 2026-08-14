package org.zstack.storage.primary.local;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 11/14/2015.
 */
@AutoQuery(replyClass = APIQueryLocalStorageResourceRefReply.class, inventoryClass = LocalStorageResourceRefInventory.class)
@RestRequest(
        path = "/primary-storage/local-storage/resource-refs",
        method = HttpMethod.GET,
        responseClass = APIQueryLocalStorageResourceRefReply.class
)
public class APIQueryLocalStorageResourceRefMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}
