package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.vm.VmCpuUtilItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/3.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusVmCpuUtilItem extends VmCpuUtilItem implements PrometheusItem {
    public PrometheusVmCpuUtilItem() {
    }

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();
        exprb.append(String.format("collectd:collectd_virt_virt_cpu_total{virt=\"%s\"}", trigger.getTargetResourceUuid()));
        exprb.append(expression.getOperator());
        exprb.append(expression.getConstant());
        builder.expression(exprb.toString());

        return builder;
    }

    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        if (!isNumber(expression.getConstant())) {
            throw new OperationFailureException(argerr("invalid right value[%s], it must be a number(int, long, float, double)", expression.getConstant()));
        }
    }

    @Override
    public AlertTextWriter createAlertTextWriter() {
        return new PrometheusVmCpuUtilAlertWriter();
    }
}
