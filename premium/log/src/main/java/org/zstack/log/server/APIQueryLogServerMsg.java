package org.zstack.log.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryLogServerReply.class, inventoryClass = LogServerInventory.class)
@RestRequest(
        path = "/log/servers",
        optionalPaths = {"/log/servers/{uuid}"},
        responseClass = APIQueryLogServerReply.class,
        method = HttpMethod.GET
)
public class APIQueryLogServerMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
