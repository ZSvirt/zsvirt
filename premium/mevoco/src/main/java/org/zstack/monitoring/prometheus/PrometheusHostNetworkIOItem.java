package org.zstack.monitoring.prometheus;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.host.HostNetworkIOItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/19.
 */
public class PrometheusHostNetworkIOItem extends HostNetworkIOItem implements PrometheusItem {
    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        String dir = (String) expression.getOrThrowExceptionIfArgumentNotInstanceOf("direction", String.class);
        if (!ALLOWED_DIRECTION.contains(dir)) {
            throw new OperationFailureException(argerr("invalid direction[%s], only %s are allowed", dir, ALLOWED_DIRECTION));
        }

        if (!isNumber(expression.getConstant())) {
            throw new OperationFailureException(argerr("invalid right value[%s], it must be a number(int, long, float, double)", expression.getConstant()));
        }
    }

    @Override
    public AlertTextWriter createAlertTextWriter() {
        return new PrometheusHostNetworkIOAlertWriter();
    }

    private String makeMetricName(String dir) {
        return String.format("collectd:collectd_interface_if_octets_%s", dir);
    }

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        String dir = (String) expression.argument("direction");

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();

        exprb.append(String.format("sum(%s{hostUuid=\"%s\"})", makeMetricName(dir), trigger.getTargetResourceUuid()));
        exprb.append(expression.getOperator());
        exprb.append(expression.getConstant());
        builder.expression(exprb.toString());

        return builder;
    }
}
