package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestRequest(
        path = "/vmSchedulingRuleGroup/{vmGroupUuid}/vmInstance/",
        method = HttpMethod.DELETE,
        responseClass = APIDetachVmFromVmSchedulingRuleGroupEvent.class
)
public class APIDetachVmFromVmSchedulingRuleGroupMsg extends APIMessage implements VmSchedulingRuleGroupMessage {
    @APIParam(resourceType = VmSchedulingRuleGroupVO.class)
    private String vmGroupUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmUuid;

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getVmSchedulingRuleGroupUuid() {
        return vmGroupUuid;
    }

    public static APIDetachVmFromVmSchedulingRuleGroupMsg __example__() {
        APIDetachVmFromVmSchedulingRuleGroupMsg msg = new APIDetachVmFromVmSchedulingRuleGroupMsg();
        msg.setVmGroupUuid(uuid());
        msg.setVmUuid(uuid());
        return msg;
    }
}
