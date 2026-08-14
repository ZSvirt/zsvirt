package org.zstack.autoscaling.group.instance;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Create by lining at 2020/03/19
 */
@TagResourceType(AutoScalingGroupInstanceVO.class)
@RestRequest(
        path = "/autoscaling/groups/instances/{instanceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateAutoScalingGroupInstanceEvent.class
)
public class APIUpdateAutoScalingGroupInstanceMsg extends APIMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String groupUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String instanceUuid;

    @APIParam(maxLength = 128,
            required = false,
            validValues = {AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_PROTECTED, AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_UNPROTECTED})
    private String protectionStrategy;

    public static APIUpdateAutoScalingGroupInstanceMsg __example__() {
        APIUpdateAutoScalingGroupInstanceMsg msg = new APIUpdateAutoScalingGroupInstanceMsg();
        msg.setProtectionStrategy(AutoScalingConstants.AutoScalingGroupInstance.PROTECTION_STRATEGY_PROTECTED);
        msg.setInstanceUuid(uuid());
        msg.setGroupUuid(uuid());

        return msg;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return groupUuid;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getProtectionStrategy() {
        return protectionStrategy;
    }

    public void setProtectionStrategy(String protectionStrategy) {
        this.protectionStrategy = protectionStrategy;
    }
}
