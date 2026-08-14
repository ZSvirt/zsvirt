package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.host.HostMemUtilItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusHostMemUtilItem extends HostMemUtilItem implements PrometheusItem {
    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        String type = (String) expression.argument("type");
        long hostTotalMem = Q.New(HostCapacityVO.class).select(HostCapacityVO_.totalMemory).eq(HostCapacityVO_.uuid, trigger.getTargetResourceUuid())
                .findValue();

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();

        double val = Double.valueOf(expression.getConstant());

        exprb.append(String.format("collectd:collectd_memory{memory=\"%s\", hostUuid=\"%s\"}", type, trigger.getTargetResourceUuid()));
        exprb.append(expression.getOperator());
        exprb.append(hostTotalMem * val);
        builder.expression(exprb.toString());

        return builder;
    }

    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        String type = (String) expression.getOrThrowExceptionIfArgumentNotInstanceOf("type", String.class);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new OperationFailureException(argerr("invalid type[%s], only %s are allowed", type, ALLOWED_TYPES));
        }

        try {
            double val = Double.valueOf(expression.getConstant());
            if (val > 1 || val < 0) {
                throw new OperationFailureException(argerr("invalid right value[%s], it must be float or double number greater than zero and lesser than one", expression.getConstant()));
            }

        } catch (NumberFormatException e) {
            throw new OperationFailureException(argerr("invalid right value[%s], it must be a float or double number", expression.getConstant()));

        }
    }

    @Override
    public AlertTextWriter createAlertTextWriter() {
        return new PrometheusHostMemUtilAlertWriter();
    }
}
