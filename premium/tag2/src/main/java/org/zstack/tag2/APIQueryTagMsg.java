package org.zstack.tag2;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagPatternInventory;

import java.util.Collections;
import java.util.List;

/**
 */
@AutoQuery(replyClass = APIQueryTagReply.class, inventoryClass = TagPatternInventory.class)
@RestRequest(
        path = "/tags",
        optionalPaths = {"/tags/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryTagReply.class
)
public class APIQueryTagMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }

}
