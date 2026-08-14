package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.host.HostCpuUtilItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/3.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusHostCpuUtilItem extends HostCpuUtilItem implements PrometheusItem {
    public PrometheusHostCpuUtilItem() {
    }

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        Long cpu = (Long) expression.argument("cpu");
        String type = (String) expression.argument("type");

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();
        if (cpu < 0) {
            if (type.equals(TYPE_IDLE)) {
                exprb.append(String.format("sum(collectd:collectd_cpu_percent{type=\"%s\", hostUuid=\"%s\"})", type, trigger.getTargetResourceUuid()));
            } else if (type.equals(TYPE_USED)) {
                exprb.append(String.format("sum(collectd:collectd_cpu_percent{type=~\"user|system\", hostUuid=\"%s\"})", trigger.getTargetResourceUuid()));
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        } else {
            if (type.equals(TYPE_IDLE)) {
                exprb.append(String.format("collectd:collectd_cpu_percent{type=\"%s\", hostUuid=\"%s\", cpu=\"%s\"}", type, trigger.getTargetResourceUuid(), cpu));
            } else if (type.equals(TYPE_USED)) {
                exprb.append(String.format("collectd:collectd_cpu_percent{type=~\"user|system\", hostUuid=\"%s\", cpu=\"%s\"}", trigger.getTargetResourceUuid(), cpu));
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        }
        exprb.append(expression.getOperator());
        exprb.append(expression.getConstant());
        builder.expression(exprb.toString());

        return builder;
    }

    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        Long cpu = (Long) expression.getOrThrowExceptionIfArgumentNotInstanceOf("cpu", Long.class);
        String type = (String) expression.getOrThrowExceptionIfArgumentNotInstanceOf("type", String.class);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new OperationFailureException(argerr("invalid type[%s], only %s are allowed", type, ALLOWED_TYPES));
        }

        int cpuNum = Q.New(HostCapacityVO.class).select(HostCapacityVO_.cpuNum)
                .eq(HostCapacityVO_.uuid, trigger.getTargetResourceUuid()).findValue();
        if (cpu >= cpuNum) {
            throw new OperationFailureException(argerr("invalid cpu[%s], the host[uuid:%s] doesn't have a CPU numbered by %s",
                    cpu, trigger.getTargetResourceUuid(), cpuNum));
        }

        if (!isNumber(expression.getConstant())) {
            throw new OperationFailureException(argerr("invalid right value[%s], it must be a number(int, long, float, double)", expression.getConstant()));
        }
    }

    @Override
    public AlertTextWriter createAlertTextWriter() {
        return new PrometheusHostCpuUtilAlertWriter();
    }
}
