package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@AutoQuery(replyClass = APIQueryMonitorTriggerReply.class, inventoryClass = MonitorTriggerInventory.class)
@RestRequest(
        path = "/monitoring/triggers",
        optionalPaths = {"/monitoring/triggers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMonitorTriggerReply.class
)
@Deprecated
public class APIQueryMonitorTriggerMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=trigger");
    }
}
