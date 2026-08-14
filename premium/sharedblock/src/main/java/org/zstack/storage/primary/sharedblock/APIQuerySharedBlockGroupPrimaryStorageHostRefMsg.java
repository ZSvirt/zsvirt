package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/sharedblock-group/host-refs",
        optionalPaths = {"/sharedblock-group/{primaryStorageUuid}/host-refs"},
        method = HttpMethod.GET,
        responseClass = APIQuerySharedBlockGroupPrimaryStorageHostRefReply.class
)
@AutoQuery(replyClass = APIQuerySharedBlockGroupPrimaryStorageHostRefReply.class, inventoryClass = SharedBlockGroupPrimaryStorageHostRefInventory.class)
public class APIQuerySharedBlockGroupPrimaryStorageHostRefMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
