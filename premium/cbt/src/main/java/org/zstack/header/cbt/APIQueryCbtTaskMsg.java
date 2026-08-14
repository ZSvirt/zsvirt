package org.zstack.header.cbt;


import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryCbtTaskReply.class, inventoryClass = CbtTaskInventory.class)
@RestRequest(
        path = "/cbt-task",
        optionalPaths = {"/cbt-task/{uuid}"},
        responseClass = APIQueryCbtTaskReply.class,
        method = HttpMethod.GET
)
public class APIQueryCbtTaskMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
