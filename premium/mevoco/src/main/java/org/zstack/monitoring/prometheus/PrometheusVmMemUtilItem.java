package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.vm.VmMemUtilItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2017/6/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusVmMemUtilItem extends VmMemUtilItem implements PrometheusItem {
    private static CLogger logger = Utils.getLogger(PrometheusVmMemUtilItem.class);

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        builder.duration(trigger.getDuration());
        StringBuilder exprb = new StringBuilder();
        double val = Double.valueOf(expression.getConstant());
        long totalMem = Q.New(VmInstanceVO.class).select(VmInstanceVO_.memorySize).eq(VmInstanceVO_.uuid, trigger.getTargetResourceUuid()).findValue();

        exprb.append(String.format("collectd:collectd_virt_memory{type=\"unused\", virt=\"%s\"}",
                trigger.getTargetResourceUuid()));
        exprb.append(expression.getOperator());
        exprb.append(totalMem * val);
        builder.expression(exprb.toString());

        return builder;
    }

    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        if (!expression.getArguments().isEmpty()) {
            throw new OperationFailureException(argerr("invalid arguments %s, no argument is allowed", expression.getArguments().keySet()));
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
        return new PrometheusVmMemUtilAlertWriter();
    }
}
