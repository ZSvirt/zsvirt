package org.zstack.monitoring.prometheus;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.vm.VmDiskIOItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/19.
 */
public class PrometheusVmDiskIOItem extends VmDiskIOItem implements PrometheusItem {
    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        String type = (String) expression.getOrThrowExceptionIfArgumentNotInstanceOf("type", String.class);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new OperationFailureException(argerr("invalid type[%s], only %s are allowed", type, ALLOWED_TYPES));
        }

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
        return new PrometheusVmDiskIOAlertWriter();
    }

    private String makeMetricName(String type, String dir) {
        String metricName = "collectd:collectd_virt_disk_";
        if (TYPE_BANDWIDTH.equals(type)) {
            metricName += "octets";
        } else if (TYPE_IOPS.equals(type)) {
            metricName += "ops";
        } else {
            throw new CloudRuntimeException(String.format("unknown type[%s]", type));
        }

        metricName += String.format("_%s", dir);
        return metricName;
    }

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        String type = (String) expression.argument("type");
        String dir = (String) expression.argument("direction");

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();

        exprb.append(String.format("sum(%s{virt=\"%s\"})", makeMetricName(type, dir), trigger.getTargetResourceUuid()));
        exprb.append(expression.getOperator());
        exprb.append(expression.getConstant());
        builder.expression(exprb.toString());

        return builder;
    }
}
