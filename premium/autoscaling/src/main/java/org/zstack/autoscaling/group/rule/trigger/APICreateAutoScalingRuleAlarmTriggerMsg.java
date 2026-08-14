package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.AlarmVO;

/**
 * Create by lining at 2018/9/16
 */
@RestRequest(
        path = "/zwatch/alarms/{alarmUuid}/autoscaling/rules/{ruleUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateAutoScalingRuleTriggerEvent.class,
        parameterName = "params"
)
public class APICreateAutoScalingRuleAlarmTriggerMsg extends APICreateAutoScalingRuleTriggerMsg {
    @APIParam(resourceType = AlarmVO.class)
    private String alarmUuid;

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    @Override
    public String getTriggerType() {
        return AutoScalingConstants.AutoScalingRule.TriggerType.Alarm;
    }

    public static APICreateAutoScalingRuleAlarmTriggerMsg __example__() {
        APICreateAutoScalingRuleAlarmTriggerMsg msg = new APICreateAutoScalingRuleAlarmTriggerMsg();
        msg.setName("createAlarmTrigger");
        msg.setAlarmUuid(uuid());
        msg.setRuleUuid(uuid());
        return msg;
    }
}
