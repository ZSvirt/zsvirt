package org.zstack.zwatch.alarm.sns;

import org.zstack.zwatch.alarm.AlarmAction;

public interface TextTemplate {
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale);

    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale);
}
