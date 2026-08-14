package org.zstack.imagereplicator;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@AutoQuery(replyClass = APIQueryImageReplicationGroupReply.class, inventoryClass = ImageReplicationGroupInventory.class)
@RestRequest(
        path = "/image-replication-groups",
        optionalPaths = {"/image-replication-groups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryImageReplicationGroupReply.class
)
public class APIQueryImageReplicationGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Arrays.asList("name=test");
    }

}
