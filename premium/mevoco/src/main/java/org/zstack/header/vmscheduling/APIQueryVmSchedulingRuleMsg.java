package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/5/5 16:42
 */
@AutoQuery(replyClass = APIQueryVmSchedulingRuleReply.class, inventoryClass = VmSchedulingRuleInventory.class)
@RestRequest(
        path = "/query/vm/schedulingRule",
        method = HttpMethod.GET,
        responseClass = APIQueryVmSchedulingRuleReply.class
)
public class APIQueryVmSchedulingRuleMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }
}
