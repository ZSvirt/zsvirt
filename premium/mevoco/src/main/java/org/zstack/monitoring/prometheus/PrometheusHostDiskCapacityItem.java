package org.zstack.monitoring.prometheus;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.host.HostDiskCapacityItem;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

import static org.zstack.monitoring.MonitorConstants.MONITOR_ITEM_TYPE_KEY;

/**
 * Created by lining on 2017/9/14.
 */
public class PrometheusHostDiskCapacityItem extends HostDiskCapacityItem implements PrometheusItem {

    @Override
    public void validateTriggerAndExpression(MonitorTriggerInventory trigger, TriggerExpression expression) {
        String type = (String) expression.getOrThrowExceptionIfArgumentNotInstanceOf(MONITOR_ITEM_TYPE_KEY, String.class);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new OperationFailureException(argerr("invalid type[%s], only %s are allowed", type, ALLOWED_TYPES));
        }

        // Do not check device, if device is null, The default check is that all disk capacity of the physical machine
    }

    @Override
    public AlertRuleWriter.RuleBuilder createAlertRule(MonitorTriggerInventory trigger, TriggerExpression expression) {
        validateTriggerAndExpression(trigger, expression);

        String devices = (String) expression.argument(DEVICES_KEY);
        String type = (String) expression.argument(MONITOR_ITEM_TYPE_KEY);
        String hostUuid = trigger.getTargetResourceUuid();

        AlertRuleWriter.RuleBuilder builder = AlertRuleWriter.newRuleBuilder();
        builder.name(makeAlertRuleName(hostUuid, trigger.getUuid()));
        builder.duration(trigger.getDuration());

        StringBuilder exprb = new StringBuilder();
        if(TYPE_AVAIL_SIZE.equals(type)){
            if(StringUtils.isEmpty(devices)){
                exprb.append(String.format("sum(node_filesystem_avail{hostUuid=\"%s\"})", hostUuid));
            }else {
                exprb.append(String.format("sum(node_filesystem_avail{hostUuid=\"%s\", device=~\"%s\"})", hostUuid, devices));
            }

        }else if(TYPE_AVAIL_PERCENT.equals(type)){
            if(StringUtils.isEmpty(devices)){
                exprb.append(String.format(
                        "sum(node_filesystem_avail{hostUuid=\"%s\"})/sum(node_filesystem_size{hostUuid=\"%s\"})",
                        hostUuid, hostUuid
                ));
            }else {
                exprb.append(String.format(
                        "sum(node_filesystem_avail{hostUuid=\"%s\", device=~\"%s\"})/sum(node_filesystem_size{hostUuid=\"%s\", device=~\"%s\"})",
                        hostUuid, devices, hostUuid, devices
                ));
            }
        }

        exprb.append(expression.getOperator());
        exprb.append(expression.getConstant());
        builder.expression(exprb.toString());

        return builder;
    }

    @Override
    public AlertTextWriter createAlertTextWriter() {
        return new PrometheusHostDiskCapacityAlertWriter();
    }
}
