package org.zstack.monitoring.prometheus;

import org.zstack.core.Platform;
import org.zstack.monitoring.MonitorConstants;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.trigger.expression.TriggerExpression;

/**
 * Created by xing5 on 2017/6/3.
 */
public interface PrometheusItem extends Item {
    default String getProvider() {
        return MonitorConstants.PROMETHEUS_PROVIDER;
    }

    AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression);

    default String makeAlertRuleName(String resourceUuid, String triggerUuid) {
        return String.format("%s.%s.trigger.%s", getName(), resourceUuid, triggerUuid).replaceAll("\\.", "_");
    }

    default String makeAlertRuleNameWithoutTriggerUuid(String resourceUuid) {
        return String.format("%s.%s", getName(), resourceUuid).replaceAll("\\.", "_");
    }

    default boolean checkIfTriggerRecovered(MonitorTriggerInventory trigger, TriggerExpression expression) {
        PrometheusMonitorProviderFactory f = Platform.getComponentLoader().getComponent(PrometheusMonitorProviderFactory.class);
        return !f.isTriggerStillProblem(trigger.getUuid());
    }

    default boolean isNumber(String num) {
        try {
            Integer.valueOf(num);
            return true;
        } catch (NumberFormatException e) {
            // not int
        }

        try {
            Long.valueOf(num);
            return true;
        } catch (NumberFormatException e) {
            // not long
        }

        try {
            Float.valueOf(num);
            return true;
        } catch (NumberFormatException e) {
            // not float
        }

        try {
            Double.valueOf(num);
            return true;
        } catch (NumberFormatException e) {
            // not double
        }

        // not a number
        return false;
    }

    String getName();
}
