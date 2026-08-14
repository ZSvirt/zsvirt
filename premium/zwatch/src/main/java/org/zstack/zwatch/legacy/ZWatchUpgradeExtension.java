package org.zstack.zwatch.legacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.identity.AccountManager;
import org.zstack.monitoring.*;
import org.zstack.monitoring.actions.EmailTriggerActionVO;
import org.zstack.monitoring.actions.EmailTriggerActionVO_;
import org.zstack.monitoring.actions.MonitorTriggerActionVO;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.media.EmailMediaVO;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import org.zstack.sns.*;
import org.zstack.sns.platform.email.SNSEmailEndpointVO;
import org.zstack.sns.platform.email.SNSEmailPlatformVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalProperty;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZWatchUpgradeExtension implements Component {
    private static CLogger logger = Utils.getLogger(ZWatchUpgradeExtension.class);

    @Autowired
    private MonitorManager monitorManager;
    @Autowired
    private ZWatchProvider provider;
    @Autowired
    private AccountManager acntMgr;

    @Override
    public boolean start() {
        if (ZWatchGlobalProperty.MIGRATE_FROM_OLD_MONITOR_IMPLEMENTATION) {
            migrateFromOldMonitorImplementation();
        }

        return true;
    }

    class AccountBundle extends SQLBatch {
        List<MonitorTriggerVO> triggers = new ArrayList<>();
        List<EmailMediaVO> medias = new ArrayList<>();
        String accountUuid;
        List<EmailTriggerActionVO> triggerActions;

        private Map<String, AlarmVO> triggerAlarmMapping = new HashMap<>();
        private Map<String, SNSEmailPlatformVO> emailMediaPlatformMapping = new HashMap<>();
        private Map<String, SNSTopicVO> triggerActionToTopicsMapping = new HashMap<>();

        private void createLabels() {
            triggers.forEach(vo -> {
                AlarmVO alarmVO = triggerAlarmMapping.get(vo.getUuid());
                TriggerExpression expression = TriggerExpression.expressionFromString(vo.getExpression());
                Item item = monitorManager.getItem(vo.getTargetResourceUuid(), expression);
                Map<String, String> labels = provider.translateToZWatchLabels(MonitorTriggerInventory.valueOf(vo), item);
                labels.forEach((k, v) -> {
                    AlarmLabelVO lvo = new AlarmLabelVO();
                    lvo.setUuid(Platform.getUuid());
                    lvo.setAlarmUuid(alarmVO.getUuid());
                    lvo.setKey(k);
                    lvo.setValue(v);
                    lvo.setOperator(Label.Operator.Equal);
                    persist(lvo);
                });
            });
        }

        private void createEmailPlatforms() {
            medias.forEach(m -> {
                SNSEmailPlatformVO evo = new SNSEmailPlatformVO();
                evo.setName(m.getName());
                evo.setState(SNSApplicationPlatformState.Enabled);
                evo.setUuid(Platform.getUuid());
                evo.setSmtpServer(m.getSmtpServer());
                evo.setPassword(m.getPassword());
                evo.setUsername(m.getUsername());
                evo.setSmtpPort(m.getSmtpPort());
                evo.setType(SNSConstants.EMAIL_PLATFORM);
                evo.setAccountUuid(accountUuid);
                persist(evo);
                emailMediaPlatformMapping.put(m.getUuid(), evo);
            });
        }

        private ComparisonOperator toComparisonOperator(TriggerExpression expression, MonitorTriggerVO vo) {
            if (expression.getOperator().equals(">")) {
                return ComparisonOperator.GreaterThan;
            } else if (expression.getOperator().equals("<")) {
                return ComparisonOperator.LessThan;
            } else if (expression.getOperator().equals(">=")) {
                return ComparisonOperator.GreaterThanOrEqualTo;
            } else if (expression.getOperator().equals("<=")) {
                return ComparisonOperator.LessThanOrEqualTo;
            } else {
                throw new CloudRuntimeException(String.format("unknown operator[%s], unable to migrate the monitor trigger: %s",
                        expression.getOperator(), JSONObjectUtil.toJsonString(MonitorTriggerInventory.valueOf(vo))));
            }
        }

        private AlarmState toState(MonitorTriggerVO vo) {
            if (vo.getState() == MonitorTriggerState.Enabled) {
                return AlarmState.Enabled;
            } else {
                return AlarmState.Disabled;
            }
        }

        private AlarmStatus toStatus(MonitorTriggerVO vo) {
            if (vo.getStatus() == MonitorTriggerStatus.OK) {
                return AlarmStatus.OK;
            } else if (vo.getStatus() == MonitorTriggerStatus.Problem) {
                return AlarmStatus.Alarm;
            } else {
                return AlarmStatus.InsufficientData;
            }
        }

        private void createAlarms() {
            triggers.forEach(vo -> {
                TriggerExpression expression = TriggerExpression.expressionFromString(vo.getExpression());
                Item item = monitorManager.getItem(vo.getTargetResourceUuid(), expression);

                AlarmVO alarmVO = new AlarmVO();
                alarmVO.setUuid(Platform.getUuid());
                alarmVO.setName(vo.getName());
                alarmVO.setDescription(vo.getDescription());
                alarmVO.setComparisonOperator(toComparisonOperator(expression, vo));

                ZWatchProvider.NamespaceMetric nm = provider.translateToZWatchTerms(item, expression);
                alarmVO.setMetricName(nm.metricName);
                alarmVO.setNamespace(nm.namespaceName);
                alarmVO.setPeriod(vo.getDuration());
                alarmVO.setStatus(toStatus(vo));
                alarmVO.setState(toState(vo));
                alarmVO.setThreshold(provider.translateToZWatchThreshold(item, expression));
                alarmVO.setType(AlarmType.Any);

                alarmVO.setCreateDate(vo.getCreateDate());
                alarmVO.setLastOpDate(vo.getLastOpDate());
                alarmVO.setAccountUuid(accountUuid);
                persist(alarmVO);

                triggerAlarmMapping.put(vo.getUuid(), alarmVO);
            });
        }

        void migrate() {
            triggerActions = q(EmailTriggerActionVO.class, MonitorTriggerActionRefVO.class, AccountResourceRefVO.class)
                    .table0().eq(EmailTriggerActionVO_.uuid).table1(MonitorTriggerActionRefVO_.actionUuid)
                    .table1().eq(MonitorTriggerActionRefVO_.triggerUuid).table2(AccountResourceRefVO_.resourceUuid)
                    .table2().eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .table2().eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .table0().selectThisTable()
                    .list();

            createEmailPlatforms();
            if (!triggers.isEmpty()) {
                createAlarms();
                createEndPointsWithTopic();
                bindEndpointsToAlarms();
                createLabels();
            }

            deleteOldData();
        }

        private void bindEndpointsToAlarms() {
            List<MonitorTriggerActionRefVO> refs = q(MonitorTriggerActionRefVO.class)
                    .in(MonitorTriggerActionRefVO_.actionUuid, triggerActions.stream().map(MonitorTriggerActionVO::getUuid).collect(Collectors.toList()))
                    .list();

            refs.forEach(ref-> {
                AlarmVO alarm = triggerAlarmMapping.get(ref.getTriggerUuid());
                SNSTopicVO topic = triggerActionToTopicsMapping.get(ref.getActionUuid());

                AlarmActionVO aavo = new AlarmActionVO();
                aavo.setActionType(SNSActionFactory.type.toString());
                aavo.setAlarmUuid(alarm.getUuid());
                aavo.setActionUuid(topic.getUuid());
                persist(aavo);
            });
        }

        private void createEndPointsWithTopic() {
            triggerActions.forEach(ta -> {
                // create email endpoints
                SNSEmailEndpointVO evo = new SNSEmailEndpointVO();
                evo.setEmail(ta.getEmail());
                evo.setUuid(Platform.getUuid());
                evo.setName(ta.getEmail());
                evo.setType(SNSConstants.EMAIL_PLATFORM);
                evo.setState(SNSApplicationEndpointState.Enabled);
                SNSEmailPlatformVO platform = emailMediaPlatformMapping.get(ta.getMediaUuid());
                DebugUtils.Assert(platform!=null, String.format("cannot find SNSEmailPlatformVO for EmailMediaVO[uuid:%s]", ta.getMediaUuid()));
                evo.setPlatformUuid(platform.getUuid());
                evo.setAccountUuid(accountUuid);
                persist(evo);

                String name = String.format("topic-for-%s", evo.getEmail());
                if (name.length() > 253) {
                    name = name.substring(0, 253);
                }

                // create one-one topic for endpoints
                SNSTopicVO tvo = new SNSTopicVO();
                tvo.setName(name);
                String desc = String.format("topic for emails: %s", evo.getEmail());
                if (desc.length() > 253) {
                    desc = desc.substring(0, 2045);
                }
                tvo.setDescription(desc);
                tvo.setState(SNSTopicState.Enabled);
                tvo.setUuid(Platform.getUuid());
                tvo.setAccountUuid(accountUuid);
                persist(tvo);

                triggerActionToTopicsMapping.put(ta.getUuid(), tvo);

                // bind endpoint and topic
                SNSSubscriberVO svo = new SNSSubscriberVO();
                svo.setTopicUuid(tvo.getUuid());
                svo.setEndpointUuid(evo.getUuid());
                persist(svo);
            });
        }

        private void deleteOldData() {
            triggers.forEach(this::remove);
            medias.forEach(this::remove);
        }

        @Override
        protected void scripts() {
            migrate();
        }
    }

    private void migrateFromOldMonitorImplementation() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                Map<String, AccountBundle> bundles = new HashMap<>();

                List<MonitorTriggerVO> triggers = q(MonitorTriggerVO.class).list();
                List<EmailMediaVO> medias = q(EmailMediaVO.class).list();

                triggers.forEach(t -> {
                    String accountUuid = acntMgr.getOwnerAccountUuidOfResource(t.getUuid());
                    DebugUtils.Assert(accountUuid!=null, String.format("cannot find accountUuid for the trigger[uuid:%s, name:%s]", t.getUuid(), t.getName()));
                    AccountBundle b = bundles.computeIfAbsent(accountUuid, k-> new AccountBundle());
                    b.accountUuid = accountUuid;
                    b.triggers.add(t);
                });

                medias.forEach(m -> {
                    String accountUuid = acntMgr.getOwnerAccountUuidOfResource(m.getUuid());
                    DebugUtils.Assert(accountUuid!=null, String.format("cannot find accountUuid for the media[uuid:%s, name:%s]", m.getUuid(), m.getName()));
                    AccountBundle b = bundles.computeIfAbsent(accountUuid, k-> new AccountBundle());
                    b.accountUuid = accountUuid;
                    b.medias.add(m);
                });

                bundles.values().forEach(AccountBundle::execute);
            }
        }.execute();
    }

    @Override
    public boolean stop() {
        return true;
    }
}
