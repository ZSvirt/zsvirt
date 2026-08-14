package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@AutoQuery(replyClass = APIQueryVmSchedHistoryReply.class, inventoryClass = VmSchedHistoryInventory.class)
@RestRequest(
        path = "/vm/sched-history",
        optionalPaths = {"/vm/sched-history/{vmInstanceUuid}"},
        responseClass = APIQueryVmSchedHistoryReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmSchedHistoryMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Arrays.asList("vmInstanceUuid=" + uuid());
    }
}
