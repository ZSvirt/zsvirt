package org.zstack.zwatch.alarm;

import java.util.List;

public interface AlarmManager {
    AlarmActionFactory getAlarmActionFactory(String type);

    void loadAlarms(List<String> alarmUuids);

    void loadAlarms(String alarmUuid);

    void loadEventSubscription(String subscriptionUuid);
}
