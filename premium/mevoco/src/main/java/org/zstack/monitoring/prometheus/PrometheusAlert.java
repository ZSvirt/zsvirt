package org.zstack.monitoring.prometheus;

import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.monitoring.MonitorTriggerContext;
import org.zstack.monitoring.MonitorTriggerVO;
import org.zstack.monitoring.MonitorTriggerVO_;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import static org.zstack.core.Platform.*;

import java.util.Map;

/**
 * Created by xing5 on 2017/6/10.
 */
public class PrometheusAlert extends MonitorTriggerContext {
    private String generatorURL;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private String startsAt;
    private String endsAt;

    public String getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(String startsAt) {
        this.startsAt = startsAt;
    }

    public String getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(String endsAt) {
        this.endsAt = endsAt;
    }

    public String getGeneratorURL() {
        return generatorURL;
    }

    public void setGeneratorURL(String generatorURL) {
        this.generatorURL = generatorURL;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }


    public String toProblemEventString() {
        return new SQLBatchWithReturn<String>() {
            @Override
            protected String scripts() {
                String resourceType = annotations.get(PrometheusMonitorProviderFactory.ITEM_RESOURCE_TYPE_ANNOTATION);
                String triggerUuid = annotations.get(PrometheusMonitorProviderFactory.TRIGGER_UUID_ANNOTATION);
                String itemName = annotations.get(PrometheusMonitorProviderFactory.ITEM_NAME_ANNOTATION);
                String value = annotations.get(PrometheusMonitorProviderFactory.VALUE_ANNOTATION);
                String resourceUuid = q(MonitorTriggerVO.class).select(MonitorTriggerVO_.targetResourceUuid).eq(MonitorTriggerVO_.uuid, triggerUuid).findValue();
                String resourceName = q(ResourceVO.class).select(ResourceVO_.resourceName).eq(ResourceVO_.uuid, resourceUuid).findValue();

                MonitorTriggerVO tvo = q(MonitorTriggerVO.class).eq(MonitorTriggerVO_.uuid, triggerUuid).find();
                TriggerExpression expression = TriggerExpression.expressionFromString(tvo.getExpression());

                return i18n("ALERT:\n " +
                        "resource[name: %s, uuid: %s, type: %s]\n" +
                        "event: %s %s %s\n" +
                        "current value: %s\n" +
                        "duration: %s seconds\n",
                        resourceName, resourceUuid, toI18nString(resourceType), itemName,
                        toI18nString(expression.getOperator()), expression.getConstant(), value, tvo.getDuration());
            }
        }.execute();
    }
}
