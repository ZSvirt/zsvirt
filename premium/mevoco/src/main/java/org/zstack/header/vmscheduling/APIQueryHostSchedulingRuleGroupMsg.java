package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/5/5 16:37
 */
@AutoQuery(replyClass = APIQueryHostSchedulingRuleGroupReply.class, inventoryClass = HostSchedulingRuleGroupInventory.class)
@RestRequest(
        path = "/query/host/schedulingRule/group",
        method = HttpMethod.GET,
        responseClass = APIQueryHostSchedulingRuleGroupReply.class
)
public class APIQueryHostSchedulingRuleGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }
}
