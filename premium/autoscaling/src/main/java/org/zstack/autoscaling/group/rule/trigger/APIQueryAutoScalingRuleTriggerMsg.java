package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Create by lining at 2018/10/10
 */
@AutoQuery(replyClass = APIQueryAutoScalingRuleTriggerReply.class, inventoryClass = AutoScalingRuleTriggerInventory.class)
@RestRequest(
        path = "/autoscaling/groups/rules/trigger",
        optionalPaths = {"/autoscaling/groups/rules/trigger/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingRuleTriggerReply.class
)
public class APIQueryAutoScalingRuleTriggerMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
