package org.zstack.monitoring.prometheus;

import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.monitoring.MonitorTriggerContext;
import org.zstack.monitoring.MonitorTriggerVO;
import org.zstack.monitoring.MonitorTriggerVO_;
import org.zstack.monitoring.items.AlertText;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/6/18.
 */
public class PrometheusVmCpuUtilAlertWriter implements AlertTextWriter {
    @Override
    public String writeProblemAlertText(MonitorTriggerContext context) {
        PrometheusAlert alert = (PrometheusAlert) context;
        Map<String, String> annotations = alert.getAnnotations();

        return new SQLBatchWithReturn<String>() {
            @Override
            protected String scripts() {
                String triggerUuid = annotations.get(PrometheusMonitorProviderFactory.TRIGGER_UUID_ANNOTATION);
                String value = annotations.get(PrometheusMonitorProviderFactory.VALUE_ANNOTATION);
                String resourceUuid = q(MonitorTriggerVO.class).select(MonitorTriggerVO_.targetResourceUuid).eq(MonitorTriggerVO_.uuid, triggerUuid).findValue();
                String resourceName = q(ResourceVO.class).select(ResourceVO_.resourceName).eq(ResourceVO_.uuid, resourceUuid).findValue();

                MonitorTriggerVO tvo = q(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, triggerUuid).find();
                TriggerExpression expression = TriggerExpression.expressionFromString(tvo.getExpression());

                AlertText text = new AlertText();
                text.resourceName = resourceName;
                text.resourceType = i18n("Virtual Machine");
                text.threshold = expression.getConstant();
                text.operator = expression.getOperator();
                text.triggerUuid = triggerUuid;
                text.value = value;
                text.problem = true;
                text.itemName = i18n("CPU Utilization");
                text.resourceUuid = resourceUuid;

                Map<String, String> args = new HashMap<>();
                args.put(i18n("CPU utilization type"), "used");
                text.args = args;
                return text.toString();
            }
        }.execute();
    }

    @Override
    public String writeOkAlertText(MonitorTriggerContext context) {
        PrometheusAlert alert = (PrometheusAlert) context;
        Map<String, String> annotations = alert.getAnnotations();

        return new SQLBatchWithReturn<String>() {
            @Override
            protected String scripts() {
                String triggerUuid = annotations.get(PrometheusMonitorProviderFactory.TRIGGER_UUID_ANNOTATION);
                String value = annotations.get(PrometheusMonitorProviderFactory.VALUE_ANNOTATION);
                String resourceUuid = q(MonitorTriggerVO.class).select(MonitorTriggerVO_.targetResourceUuid).eq(MonitorTriggerVO_.uuid, triggerUuid).findValue();
                String resourceName = q(ResourceVO.class).select(ResourceVO_.resourceName).eq(ResourceVO_.uuid, resourceUuid).findValue();

                MonitorTriggerVO tvo = q(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, triggerUuid).find();
                TriggerExpression expression = TriggerExpression.expressionFromString(tvo.getExpression());

                AlertText text = new AlertText();
                text.resourceName = resourceName;
                text.resourceType = i18n("Virtual Machine");
                text.resourceUuid = resourceUuid;
                text.threshold = expression.getConstant();
                text.operator = expression.getOperator();
                text.triggerUuid = triggerUuid;
                text.value = value;
                text.problem = false;
                text.itemName = i18n("CPU Utilization");

                Map<String, String> args = new HashMap<>();
                args.put(i18n("CPU utilization type"), "used");
                text.args = args;
                return text.toString();
            }
        }.execute();
    }
}
