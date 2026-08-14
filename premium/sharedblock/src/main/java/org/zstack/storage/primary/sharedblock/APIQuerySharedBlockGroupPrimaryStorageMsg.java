package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/primary-storage/sharedblockgroup",
        optionalPaths = {"/primary-storage/sharedblockgroup/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySharedBlockGroupPrimaryStorageReply.class
)
@AutoQuery(replyClass = APIQuerySharedBlockGroupPrimaryStorageReply.class, inventoryClass = SharedBlockGroupPrimaryStorageInventory.class)
public class APIQuerySharedBlockGroupPrimaryStorageMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
