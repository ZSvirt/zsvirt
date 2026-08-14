package org.zstack.zwatch.header;

import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.AccountConstant;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmVO;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmVO_;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.alarm.system.AlarmSystemTagUtils;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.ArrayList;
import java.util.Objects;

public interface AlarmDatabasePrepareHandler {
    class PredefinedSystemAlarmBuilder {
        private String uuid;
        private String name;
        private String nameCN;
        private String metricName;
        private ComparisonOperator comparisonOperator;
        private int threshold;
        private int period;
        private int repeatInterval;
        private int repeatCount;
        private AlarmType alarmType;
        private EmergencyLevel emergencyLevel;
        private String activeAlarmTemplateUuid;
        private String namespace;
        private final ArrayList<Label> labels = new ArrayList<>();

        public PredefinedSystemAlarmBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public PredefinedSystemAlarmBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PredefinedSystemAlarmBuilder nameCN(String nameCN) {
            this.nameCN = nameCN;
            return this;
        }

        public PredefinedSystemAlarmBuilder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public PredefinedSystemAlarmBuilder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = comparisonOperator;
            return this;
        }

        public PredefinedSystemAlarmBuilder threshold(int threshold) {
            this.threshold = threshold;
            return this;
        }

        public PredefinedSystemAlarmBuilder period(int period) {
            this.period = period;
            return this;
        }

        public PredefinedSystemAlarmBuilder repeatInterval(int repeatInterval) {
            this.repeatInterval = repeatInterval;
            return this;
        }

        public PredefinedSystemAlarmBuilder repeatCount(int repeatCount) {
            this.repeatCount = repeatCount;
            return this;
        }

        public PredefinedSystemAlarmBuilder alarmType(AlarmType alarmType) {
            this.alarmType = alarmType;
            return this;
        }

        public PredefinedSystemAlarmBuilder emergencyLevel(EmergencyLevel emergencyLevel) {
            this.emergencyLevel = emergencyLevel;
            return this;
        }

        public PredefinedSystemAlarmBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public PredefinedSystemAlarmBuilder label(Label label) {
            this.labels.add(label);
            return this;
        }

        public PredefinedSystemAlarmBuilder activeAlarmTemplateUuid(String activeAlarmTemplateUuid) {
            this.activeAlarmTemplateUuid = activeAlarmTemplateUuid;
            return this;
        }

        public void reset() {
            uuid = null;
            name = null;
            nameCN = null;
            metricName = null;
            comparisonOperator = null;
            threshold = 0;
            period = 0;
            repeatInterval = 0;
            repeatCount = 0;
            alarmType = null;
            emergencyLevel = null;
            namespace = null;
            labels.clear();
            activeAlarmTemplateUuid = null;
        }

        public void build() {
            DatabaseFacade database = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

            AlarmVO alarm = database.findByUuid(uuid, AlarmVO.class);
            boolean newCreated = alarm == null;
            if (newCreated) {
                alarm = new AlarmVO();
                alarm.setUuid(uuid);
                alarm.setName(name);
                alarm.setNamespace(namespace);
                alarm.setRepeatInterval(repeatInterval);
                alarm.setState(AlarmState.Enabled);
                alarm.setStatus(AlarmStatus.OK);
                alarm.setThreshold(threshold);
                alarm.setPeriod(period);
                alarm.setMetricName(metricName);
                alarm.setComparisonOperator(comparisonOperator);
                alarm.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                alarm.setType(alarmType);
                alarm.setRepeatCount(repeatCount);
                alarm.setEmergencyLevel(emergencyLevel);
                database.persist(alarm);
            } else if (!Objects.equals(alarm.getName(), name)) {
                SQL.New(AlarmVO.class)
                        .eq(AlarmVO_.uuid, uuid)
                        .set(AlarmVO_.name, name)
                        .update();
            }

            if (!Q.New(AlarmActionVO.class)
                    .eq(AlarmActionVO_.alarmUuid, uuid)
                    .eq(AlarmActionVO_.actionUuid, SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID)
                    .isExists()) {
                AlarmActionVO action = new AlarmActionVO();
                action.setActionUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID);
                action.setActionType(SNSActionFactory.type.toString());
                action.setAlarmUuid(uuid);
                database.persist(action);
            }

            if (activeAlarmTemplateUuid != null
                    && !Q.New(ActiveAlarmVO.class)
                    .eq(ActiveAlarmVO_.alarmUuid, uuid)
                    .isExists()) {
                ActiveAlarmVO activeAlarmVO = new ActiveAlarmVO();
                activeAlarmVO.setUuid(Platform.getUuid());
                activeAlarmVO.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                activeAlarmVO.setAlarmUuid(uuid);
                activeAlarmVO.setNamespace(namespace);
                activeAlarmVO.setTemplateUuid(activeAlarmTemplateUuid);
                database.persist(activeAlarmVO);
            }

            labels.forEach(label -> {
                if (!Q.New(AlarmLabelVO.class)
                        .eq(AlarmLabelVO_.alarmUuid, uuid)
                        .eq(AlarmLabelVO_.key, label.getKey())
                        .isExists()) {
                    AlarmLabelVO labelVO = new AlarmLabelVO();
                    labelVO.setUuid(Platform.getUuid());
                    labelVO.setOperator(label.getOp());
                    labelVO.setKey(label.getKey());
                    labelVO.setValue(label.getValue());
                    labelVO.setAlarmUuid(uuid);
                    database.persist(labelVO);
                }
            });

            AlarmSystemTagUtils.persistSystemTagOfLanguage(alarm, AlarmSystemTags.CN, nameCN);
        }
    }

    class PredefinedEventSubscriptionBuilder {
        private String uuid;
        private String name;
        private String nameCN;
        private String eventName;
        private String namespace;
        private EmergencyLevel emergencyLevel;
        private EventSubscriptionState state;

        public PredefinedEventSubscriptionBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public PredefinedEventSubscriptionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PredefinedEventSubscriptionBuilder nameCN(String nameCN) {
            this.nameCN = nameCN;
            return this;
        }

        public PredefinedEventSubscriptionBuilder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public PredefinedEventSubscriptionBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public PredefinedEventSubscriptionBuilder emergencyLevel(EmergencyLevel emergencyLevel) {
            this.emergencyLevel = emergencyLevel;
            return this;
        }

        public PredefinedEventSubscriptionBuilder state(EventSubscriptionState state) {
            this.state = state;
            return this;
        }

        public void reset() {
            uuid = null;
            name = null;
            nameCN = null;
            eventName = null;
            namespace = null;
            emergencyLevel = null;
            state = null;
        }

        public void build() {
            DatabaseFacade database = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

            EventSubscriptionVO eventSubscriptionVO = database.findByUuid(uuid, EventSubscriptionVO.class);
            boolean newCreated = eventSubscriptionVO == null;

            if (newCreated) {
                eventSubscriptionVO = new EventSubscriptionVO();
                eventSubscriptionVO.setUuid(uuid);
                eventSubscriptionVO.setName(name);
                eventSubscriptionVO.setEventName(eventName);
                eventSubscriptionVO.setNamespace(namespace);
                eventSubscriptionVO.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                eventSubscriptionVO.setState(state == null ? EventSubscriptionState.Enabled : state);
                eventSubscriptionVO.setEmergencyLevel(emergencyLevel);
                database.persist(eventSubscriptionVO);
            }

            if (!Q.New(EventSubscriptionActionVO.class)
                    .eq(EventSubscriptionActionVO_.subscriptionUuid, uuid)
                    .eq(AlarmActionVO_.actionUuid, SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID)
                    .isExists()) {
                EventSubscriptionActionVO action = new EventSubscriptionActionVO();
                action.setActionUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID);
                action.setActionType(SNSActionFactory.type.toString());
                action.setSubscriptionUuid(uuid);
                database.persist(action);
            }

            AlarmSystemTagUtils.persistSystemTagOfLanguage(eventSubscriptionVO, EventSubscriptionSystemTags.CN, nameCN);
        }
    }
}
