package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmPriorityConfigReply.class, inventoryClass = VmPriorityConfigInventory.class)
@RestRequest(
        path = "/vm-priority-config",
        optionalPaths = {"/vm-priority-config/{uuid}"},
        responseClass = APIQueryVmPriorityConfigReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmPriorityConfigMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
