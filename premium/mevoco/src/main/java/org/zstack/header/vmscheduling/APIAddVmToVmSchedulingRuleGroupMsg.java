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
        path = "/vmSchedulingRuleGroup/{vmGroupUuid}/vmInstance/{vmUuid}",
        method = HttpMethod.POST,
        responseClass = APIAddVmToVmSchedulingRuleGroupEvent.class
)
public class APIAddVmToVmSchedulingRuleGroupMsg extends APIMessage implements VmSchedulingRuleGroupMessage {
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

    public static APIAddVmToVmSchedulingRuleGroupMsg __example__() {
        APIAddVmToVmSchedulingRuleGroupMsg msg = new APIAddVmToVmSchedulingRuleGroupMsg();
        msg.setVmGroupUuid(uuid());
        msg.setVmUuid(uuid());
        return msg;
    }
}
