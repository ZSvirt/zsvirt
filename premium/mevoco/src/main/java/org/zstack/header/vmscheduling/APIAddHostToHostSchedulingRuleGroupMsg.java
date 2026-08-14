package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/12/2
 */
@RestRequest(
        path = "/hostSchedulingRuleGroup/{hostGroupUuid}/host/{hostUuid}",
        method = HttpMethod.POST,
        responseClass = APIAddHostToHostSchedulingRuleGroupEvent.class
)
public class APIAddHostToHostSchedulingRuleGroupMsg extends APIMessage implements HostSchedulingRuleGroupMessage {
    @APIParam(resourceType = HostSchedulingRuleGroupVO.class)
    private String hostGroupUuid;

    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public static APIAddHostToHostSchedulingRuleGroupMsg __example__() {
        APIAddHostToHostSchedulingRuleGroupMsg msg = new APIAddHostToHostSchedulingRuleGroupMsg();
        msg.setHostGroupUuid(uuid());
        msg.setHostGroupUuid(uuid());
        return msg;
    }
}
