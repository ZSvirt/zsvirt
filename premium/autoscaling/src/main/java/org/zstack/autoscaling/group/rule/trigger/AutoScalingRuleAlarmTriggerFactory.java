package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.zwatch.alarm.AlarmConstants;
import org.zstack.zwatch.message.AlarmDeletionMsg;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/18 2:30 PM
 */
public class AutoScalingRuleAlarmTriggerFactory implements AutoScalingRuleTriggerFactory {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;


    @Override
    public AutoScalingRuleTriggerType getType() {
        return AutoScalingRuleTriggerType.Alarm;
    }

    @Override
    public String getResourceUuid(String triggerUuid) {
        return dbf.findByUuid(triggerUuid, AutoScalingRuleAlarmTriggerVO.class).getAlarmUuid();
    }

    @Override
    public void cleanResource(String alarmUuid, Completion completion) {
        AlarmDeletionMsg alarmDeletionMsg = new AlarmDeletionMsg();
        alarmDeletionMsg.setUuid(alarmUuid);
        bus.makeTargetServiceIdByResourceUuid(alarmDeletionMsg, AlarmConstants.SERVICE_ID, alarmUuid);
        bus.send(alarmDeletionMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
