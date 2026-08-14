package org.zstack.monitoring;

import org.zstack.header.core.Completion;
import org.zstack.monitoring.actions.MonitorTriggerActionInventory;

/**
 * Created by xing5 on 2017/6/12.
 */
public class NotificationTriggerEvent extends TextBasedTriggerEvent {
    public NotificationTriggerEvent(MonitorTriggerInventory trigger, MonitorTriggerActionInventory action, MonitorTriggerContext context) {
        super(trigger, action, context);
    }

    @Override
    public void triggerOK(Completion completion) {
        completion.success();
    }

    @Override
    public void triggerProblem(Completion completion) {
        completion.success();
    }
}
