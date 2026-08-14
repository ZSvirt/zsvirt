package org.zstack.zwatch.alarm.sns;

import org.zstack.zwatch.alarm.AlarmAction;

public interface EventTextTemplate {
    SNSTopicMessage createEvent(AlarmAction.TakeEventSubscriptionActionParam actionParam);
}
