package org.zstack.zwatch.alarm;

import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

import java.util.Collection;

public interface AlarmActionFactory {
    String getActionType();

    AlarmAction createAlarmAction(String actionUuid);

    TextTemplateFactory getTextTemplateFactory(String type);

    Collection<TextTemplateFactory> getAllTextTemplateFactory();

    boolean isActionExists(String actionUuid);
}
