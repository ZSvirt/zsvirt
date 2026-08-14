package org.zstack.zwatch.alarm.sns;

import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.namespace.ThirdpartyAlertNamespace;

import java.util.Map;

import static org.zstack.core.Platform.i18n;

public abstract class ThirdpartyAbstractTextTemplate extends AbstractTextTemplate {

    @Override
    protected Map<String, Object> makeTemplateBindings(AlarmAction.TakeEventSubscriptionActionParam param, boolean doi18nTranslate) {
        Map<String, Object> bindings = super.makeTemplateBindings(param, true);

        String alertLevel = param.event.getLabels().get(ThirdpartyAlertNamespace.EventLabelNames.AlertLevel.toString());
        String emergencyLevelValue;
        if (doi18nTranslate) {
            emergencyLevelValue = i18n(alertLevel);
        } else {
            emergencyLevelValue = alertLevel;
        }
        bindings.put(PARAM_EVENT_EMERGENCY_LEVEL, emergencyLevelValue);

        return bindings;
    }
}

