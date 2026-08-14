package org.zstack.monitoring;

import org.zstack.header.core.Completion;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.targets.MonitorTarget;
import org.zstack.monitoring.trigger.expression.TriggerExpression;

/**
 * Created by xing5 on 2017/6/3.
 */
public interface MonitorProvider {
    void setupMonitor(MonitorTarget template, Completion completion);

    void deleteMonitor(MonitorTarget template, Completion completion);

    void createTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression triggerExpression, Completion completion);

    void deleteTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression expression, Completion completion);
}
