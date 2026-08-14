package org.zstack.core.eventlog;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/eventlogs",
        optionalPaths = {"/eventlogs/{id}"},
        method = HttpMethod.GET,
        responseClass = APIQueryEventLogReply.class
)
@AutoQuery(replyClass = APIQueryEventLogReply.class, inventoryClass = EventLogInventory.class)
public class APIQueryEventLogMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
