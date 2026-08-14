package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Create by weiwang at 2018/8/16
 */
@AutoQuery(replyClass = APIQueryAutoScalingRuleReply.class, inventoryClass = AutoScalingRuleInventory.class)
@RestRequest(
        path = "/autoscaling/groups/rules",
        optionalPaths = {"/autoscaling/groups/rules/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingRuleReply.class
)
public class APIQueryAutoScalingRuleMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
