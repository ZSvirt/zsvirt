package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.affinitygroup.APIDeleteAffinityGroupEvent;
import org.zstack.header.affinitygroup.APIDeleteAffinityGroupMsg;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@RestRequest(
        path = "/vmSchedulingRule/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAffinityGroupEvent.class
)
public class APIRemoveVmSchedulingRuleMsg extends APIDeleteAffinityGroupMsg {
    
    public static APIRemoveVmSchedulingRuleMsg __example__() {
        APIRemoveVmSchedulingRuleMsg msg = new APIRemoveVmSchedulingRuleMsg();
        msg.setUuid(uuid());
        return msg;
    }

}
