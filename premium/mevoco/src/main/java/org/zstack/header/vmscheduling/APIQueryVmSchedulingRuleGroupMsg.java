package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/5/5 16:35
 */
@AutoQuery(replyClass = APIQueryVmSchedulingRuleGroupReply.class, inventoryClass = VmSchedulingRuleGroupInventory.class)
@RestRequest(
        path = "/query/vm/schedulingRule/group",
        method = HttpMethod.GET,
        responseClass = APIQueryVmSchedulingRuleGroupReply.class
)
public class APIQueryVmSchedulingRuleGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }
}
