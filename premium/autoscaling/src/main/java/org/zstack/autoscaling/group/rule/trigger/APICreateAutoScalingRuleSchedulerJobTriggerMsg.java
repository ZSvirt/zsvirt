package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.http.*;
import org.zstack.autoscaling.*;
import org.zstack.header.message.*;
import org.zstack.header.rest.*;
import org.zstack.header.scheduler.*;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/8 11:04 AM
 */
@RestRequest(
        path = "/scheduler/jobs/{schedulerJobUuid}/autoscaling/rules/{ruleUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateAutoScalingRuleTriggerEvent.class,
        parameterName = "params"
)
public class APICreateAutoScalingRuleSchedulerJobTriggerMsg extends APICreateAutoScalingRuleTriggerMsg {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String schedulerJobUuid;

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    @Override
    public String getTriggerType() {
        return AutoScalingConstants.AutoScalingRule.TriggerType.TimedTask;
    }

    public static APICreateAutoScalingRuleSchedulerJobTriggerMsg __example__() {
        APICreateAutoScalingRuleSchedulerJobTriggerMsg msg = new APICreateAutoScalingRuleSchedulerJobTriggerMsg();
        msg.setName("createAlarmTrigger");
        msg.setSchedulerJobUuid(uuid());
        msg.setRuleUuid(uuid());
        return msg;
    }
}
