package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/sharedblock-group/sharedblocks",
        optionalPaths = {"/sharedblock-group",
                         "/sharedblock-group/sharedblock/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySharedBlockReply.class
)
@AutoQuery(replyClass = APIQuerySharedBlockReply.class, inventoryClass = SharedBlockInventory.class)
public class APIQuerySharedBlockMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
