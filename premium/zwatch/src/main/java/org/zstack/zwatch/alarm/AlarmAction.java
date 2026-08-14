package org.zstack.zwatch.alarm;

import org.zstack.zwatch.datatype.EventData;
import org.zstack.zwatch.datatype.Namespace;

public interface AlarmAction {
    class TakeAlarmActionParam {
        public AlarmInventory alarm;
        public AlarmStatus previousStatus;
        public AlarmStatus currentStatus;
        public double currentValue;
        public String identifyLabel;
        public String alarmAccountUuid;
        public String alarmDataUuid;
        public long timeMillis;

        public String getNamespace(String resourceUuid) {
            Namespace ns = Namespace.getMetricNameSpace(alarm.getNamespace(), alarm.getMetricName());
            return ns.getAlarmNameSpace(resourceUuid, alarm.getUuid(), alarm.getNamespace());
        }

        public String getI18nMetric(String resourceUuid, String i18nMetricName) {
            Namespace ns = Namespace.getMetricNameSpace(alarm.getNamespace(), alarm.getMetricName());
            return ns.getAlarmI18nMetric(resourceUuid, alarm.getUuid(), i18nMetricName);
        }
    }

    class TakeEventSubscriptionActionParam {
        public EventData event;
        public String subscriptionUuid;
        public String subscriptionAccountUuid;
        public String dataUuid;

        public String getNamespace(String resourceUuid) {
            Namespace ns = Namespace.getEventNameSpace(event.getNamespace(), event.getName());
            return ns.getEventNameSpace(resourceUuid, subscriptionUuid, event.getNamespace());
        }
    }

    void takeAction(TakeAlarmActionParam param);

    void takeAction(TakeEventSubscriptionActionParam param);

    void takeActionForThirdpartyAlert(TakeEventSubscriptionActionParam param);
}
