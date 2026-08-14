package org.zstack.monitoring.prometheus;

import org.apache.commons.lang.StringUtils;
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

import static org.zstack.monitoring.MonitorConstants.MONITOR_ITEM_TYPE_KEY;
import static org.zstack.monitoring.items.host.HostDiskCapacityItem.DEVICES_KEY;

/**
 * Created by lining on 2017/9/14.
 */
public class PrometheusHostDiskCapacityAlertWriter implements AlertTextWriter {
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
                text.resourceType = i18n("Host");
                text.resourceUuid = resourceUuid;
                text.threshold = expression.getConstant();
                text.operator = expression.getOperator();
                text.triggerUuid = triggerUuid;
                text.value = value;
                text.problem = true;
                text.itemName = i18n("Host Disk Capacity");

                String devices = (String) expression.argument(DEVICES_KEY);
                String type = (String) expression.argument(MONITOR_ITEM_TYPE_KEY);

                Map<String, String> args = new HashMap<>();
                args.put(i18n("Host Disk Capacity type"), type);
                if(StringUtils.isNotEmpty(devices)){
                    args.put(i18n("Host devices"), devices);
                }

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
                text.resourceType = i18n("Host");
                text.resourceUuid = resourceUuid;
                text.threshold = expression.getConstant();
                text.operator = expression.getOperator();
                text.triggerUuid = triggerUuid;
                text.value = value;
                text.problem = false;
                text.itemName = i18n("Host Disk Capacity");

                String devices = (String) expression.argument(DEVICES_KEY);
                String type = (String) expression.argument(MONITOR_ITEM_TYPE_KEY);

                Map<String, String> args = new HashMap<>();
                args.put(i18n("Host Disk Capacity type"), type);
                if(StringUtils.isNotEmpty(devices)){
                    args.put(i18n("Host devices"), devices);
                }

                text.args = args;
                return text.toString();
            }
        }.execute();
    }
}
