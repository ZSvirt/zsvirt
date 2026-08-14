package org.zstack.monitoring;

import org.zstack.monitoring.actions.MonitorTriggerActionFactory;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.trigger.expression.TriggerExpression;

/**
 * Created by xing5 on 2017/6/3.
 */
public interface MonitorManager {
    MonitorProviderFactory getMonitorProviderFactory(String type);

    MonitorTriggerActionFactory getMonitorActionFactory(String type);

    Item getItem(String resourceUuid, TriggerExpression triggerExpression);

    Item getItemByExpressionString(String expressionString);
}
