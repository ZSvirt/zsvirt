package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/12/2
 */
@RestRequest(
        path = "/hostSchedulingRuleGroup/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteHostSchedulingRuleGroupEvent.class
)
public class APIDeleteHostSchedulingRuleGroupMsg extends APIMessage implements HostSchedulingRuleGroupMessage {
    @APIParam(resourceType = HostSchedulingRuleGroupVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostGroupUuid() {
        return uuid;
    }

    public static APIDeleteHostSchedulingRuleGroupMsg __example__() {
        APIDeleteHostSchedulingRuleGroupMsg msg = new APIDeleteHostSchedulingRuleGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
