package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/7/8.
 */
@AutoQuery(replyClass = APIQueryMonitorTriggerActionReply.class, inventoryClass = EmailTriggerActionInventory.class)
@RestRequest(
        path = "/monitoring/trigger-actions/emails",
        optionalPaths = {"/monitoring/trigger-actions/emails/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMonitorTriggerActionReply.class
)
@Deprecated
public class APIQueryEmailTriggerActionMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=email");
    }
}
