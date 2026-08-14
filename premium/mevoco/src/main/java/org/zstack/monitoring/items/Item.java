package org.zstack.monitoring.items;

import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.trigger.expression.TriggerExpression;

/**
 * Created by xing5 on 2017/6/2.
 */
public interface Item {
    String getResourceType();
    String getName();
    String getReadableName();
    String getProvider();
    void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression);
    boolean checkIfTriggerRecovered(MonitorTriggerInventory trigger, TriggerExpression expression);
    AlertTextWriter createAlertTextWriter();
}
