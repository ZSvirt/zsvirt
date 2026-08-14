package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestRequest(
        path = "/monitoring/trigger-actions",
        optionalPaths = {"/monitoring/trigger-actions/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMonitorTriggerActionReply.class
)
@AutoQuery(replyClass = APIQueryMonitorTriggerActionReply.class, inventoryClass = MonitorTriggerActionInventory.class)
@Deprecated
public class APIQueryMonitorTriggerActionMsg extends APIQueryMessage {
    public static List<String> __example__()  {
        return asList("name=email");
    }
}
