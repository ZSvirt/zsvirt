package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestRequest(
        path = "/vmSchedulingRuleGroup/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmSchedulingRuleGroupEvent.class
)
public class APIDeleteVmSchedulingRuleGroupMsg extends APIMessage implements VmSchedulingRuleGroupMessage {
    @APIParam(resourceType = VmSchedulingRuleGroupVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmSchedulingRuleGroupUuid() {
        return uuid;
    }

    public static APIDeleteVmSchedulingRuleGroupMsg __example__() {
        APIDeleteVmSchedulingRuleGroupMsg msg = new APIDeleteVmSchedulingRuleGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
