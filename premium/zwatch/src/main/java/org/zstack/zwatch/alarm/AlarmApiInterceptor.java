package org.zstack.zwatch.alarm;

import java.util.Arrays;
import java.util.List;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.Account;
import org.zstack.zwatch.alarm.activealarm.api.APIChangeActiveAlarmStateMsg;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateVO;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateVO_;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.monitorgroup.api.*;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateVO;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateVO;
import org.zstack.zwatch.mysql.MysqlDatabaseDriver;
import org.zstack.zwatch.prometheus.PrometheusDatabaseDriver;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import java.util.*;

@InterceptorForService("zwatch.alarm")
public class AlarmApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private AlarmManager alarmManager;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static Set<String> defaultLabelNames = new HashSet<>(Arrays.asList(EventData.LABEL_RESOURCE_ID, EventData.LABEL_EMERGENCY_LEVEL));

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIRemoveLabelFromAlarmMsg) {
            validate((APIRemoveLabelFromAlarmMsg) msg);
        } else if (msg instanceof APIAddLabelToAlarmMsg) {
            validate((APIAddLabelToAlarmMsg) msg);
        } else if (msg instanceof APIAddActionToEventSubscriptionMsg) {
            validate((APIAddActionToEventSubscriptionMsg) msg);
        } else if (msg instanceof APIAddLabelToEventSubscriptionMsg) {
            validate((APIAddLabelToEventSubscriptionMsg) msg);
        } else if (msg instanceof APIRemoveLabelFromEventSubscriptionMsg) {
            validate((APIRemoveLabelFromEventSubscriptionMsg) msg);
        } else if (msg instanceof APIAddActionToAlarmMsg) {
            validate((APIAddActionToAlarmMsg) msg);
        } else if (msg instanceof APICreateAlarmMsg) {
            validate((APICreateAlarmMsg) msg);
        } else if (msg instanceof APISubscribeEventMsg) {
            validate((APISubscribeEventMsg) msg);
        } else if (msg instanceof APIUpdateAlarmLabelMsg) {
            validate((APIUpdateAlarmLabelMsg) msg);
        } else if (msg instanceof APIUpdateEventSubscriptionLabelMsg) {
            validate((APIUpdateEventSubscriptionLabelMsg) msg);
        } else if (msg instanceof APIUpdateAlarmMsg) {
            validate((APIUpdateAlarmMsg) msg);
        } else if (msg instanceof APICreateMonitorGroupMsg) {
            validate((APICreateMonitorGroupMsg) msg);
        } else if (msg instanceof APIUpdateMonitorGroupMsg) {
            validate((APIUpdateMonitorGroupMsg) msg);
        } else if (msg instanceof APIAddMetricRuleTemplateMsg) {
            validate((APIAddMetricRuleTemplateMsg) msg);
        } else if (msg instanceof APIUpdateMetricRuleTemplateMsg) {
            validate((APIUpdateMetricRuleTemplateMsg) msg);
        } else if (msg instanceof APIAddEventRuleTemplateMsg) {
            validate((APIAddEventRuleTemplateMsg) msg);
        } else if (msg instanceof APIUpdateEventRuleTemplateMsg) {
            validate((APIUpdateEventRuleTemplateMsg) msg);
        }else if (msg instanceof APIChangeActiveAlarmStateMsg) {
            validate((APIChangeActiveAlarmStateMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof AlarmMessage) {
            AlarmMessage amsg = (AlarmMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, amsg.getAlarmUuid());
        } else if (msg instanceof EventSubscriptionMessage) {
            EventSubscriptionMessage emsg = (EventSubscriptionMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AlarmConstants.SERVICE_ID, emsg.getSubscriptionUuid());
        }
    }

    private void validate(APIUpdateAlarmLabelMsg msg) {
        String alarmUuid = Q.New(AlarmLabelVO.class)
                .select(AlarmLabelVO_.alarmUuid)
                .eq(AlarmLabelVO_.uuid, msg.getUuid())
                .findValue();

        msg.setAlarmUuid(alarmUuid);
    }

    private void validate(APIUpdateEventSubscriptionLabelMsg msg) {
        String eventSubscriptionUuid = Q.New(EventSubscriptionLabelVO.class)
                .select(EventSubscriptionLabelVO_.subscriptionUuid)
                .eq(EventSubscriptionLabelVO_.uuid, msg.getUuid())
                .findValue();

        EventSubscriptionVO vo = dbf.findByUuid(eventSubscriptionUuid, EventSubscriptionVO.class);
        Namespace ns = Namespace.getEventNameSpace(vo.getNamespace(), vo.getEventName());
        boolean has = false;
        for (EventFamily ef : ns.getEvents()) {
            if (ef.getLabelNames().contains(msg.getKey())) {
                has = true;
                break;
            }
        }

        if (!has && !defaultLabelNames.contains(msg.getKey())) {
            throw new ApiMessageInterceptionException(argerr("event doesn't have label[%s]", msg.getKey()));
        }

        if (Q.New(EventSubscriptionLabelVO.class)
                .eq(EventSubscriptionLabelVO_.subscriptionUuid, msg.getSubscriptionUuid())
                .notEq(EventSubscriptionLabelVO_.uuid, msg.getUuid())
                .eq(EventSubscriptionLabelVO_.key, msg.getKey()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("the event subscription already has the label[%s]", msg.getKey()));
        }

        msg.setSubscriptionUuid(eventSubscriptionUuid);
    }

    private void validate(APIAddActionToEventSubscriptionMsg msg) {
        if (Q.New(EventSubscriptionActionVO.class).eq(EventSubscriptionActionVO_.subscriptionUuid, msg.getSubscriptionUuid())
                .eq(EventSubscriptionActionVO_.actionUuid, msg.getActionUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("the action[uuid:%s] already attached to the event subscription[uuid:%s]",
                    msg.getActionUuid(), msg.getSubscriptionUuid()));
        }
        checkAction(msg.getActionUuid(), msg.getActionType());
    }

    private void validate(APIRemoveLabelFromEventSubscriptionMsg msg) {
        msg.setSubscriptionUuid(
                Q.New(EventSubscriptionLabelVO.class).select(EventSubscriptionLabelVO_.subscriptionUuid)
                        .eq(EventSubscriptionLabelVO_.uuid, msg.getUuid()).findValue()
        );
    }

    private void validate(APIAddLabelToEventSubscriptionMsg msg) {
        EventSubscriptionVO vo = dbf.findByUuid(msg.getSubscriptionUuid(), EventSubscriptionVO.class);
        Namespace ns = Namespace.getEventNameSpace(vo.getNamespace(), vo.getEventName());
        boolean has = false;
        for (EventFamily ef : ns.getEvents()) {
            if (ef.getLabelNames().contains(msg.getKey())) {
                has = true;
                break;
            }
        }

        if (!has && !defaultLabelNames.contains(msg.getKey())) {
            throw new ApiMessageInterceptionException(argerr("event doesn't have label[%s]", msg.getKey()));
        }

        if (Q.New(EventSubscriptionLabelVO.class).eq(EventSubscriptionLabelVO_.subscriptionUuid, msg.getSubscriptionUuid())
                .eq(EventSubscriptionLabelVO_.key, msg.getKey()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("the event subscription already has the label[%s]", msg.getKey()));
        }
    }

    private void validate(APISubscribeEventMsg msg) {
        if (!Namespace.namespaces.containsKey(msg.getNamespace())) {
            throw new ApiMessageInterceptionException(argerr("namespace[%s] not found", msg.getNamespace()));
        }

        Namespace ns = Namespace.getEventNameSpace(msg.getNamespace(), msg.getEventName());
        EventFamily ef = null;
        if (ns.getEvents() != null) {
            Optional<EventFamily> opt = ns.getEvents().stream().filter(e -> e.getName().equals(msg.getEventName())).findFirst();
            if (!opt.isPresent()) {
                throw new ApiMessageInterceptionException(argerr("namespace[%s] doesn't have the event[%s]", ns.getName(), msg.getEventName()));
            }
            ef = opt.get();
        }

        if (ef == null) {
            throw new ApiMessageInterceptionException(argerr("namespace[%s] doesn't have the event[%s]", ns.getName(), msg.getEventName()));
        }

        if (msg.getLabels() != null) {
            Map<String, List> dup = new HashMap<>();
            EventFamily finalEf = ef;
            msg.getLabels().forEach(l -> {
                if (!finalEf.getLabelNames().contains(l.getKey()) && !defaultLabelNames.contains(l.getKey())) {
                    throw new ApiMessageInterceptionException(argerr("event[%s] doesn't have the label[%s]", msg.getEventName(), l.getKey()));
                }

                l.check();
                List lst = dup.computeIfAbsent(l.getKey(), x->new ArrayList());
                lst.add(l.getValue());
            });

            dup.forEach((k, l)-> {
                if (l.size() > 1) {
                    throw new ApiMessageInterceptionException(argerr("duplicate key[%s] with values%s", k, l));
                }
            });
        }
    }

    private void validate(APICreateAlarmMsg msg) {
        if (!Namespace.namespaces.containsKey(msg.getNamespace())) {
            throw new ApiMessageInterceptionException(argerr("namespace[%s] not found", msg.getNamespace()));
        }

        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());

        if (PrometheusDatabaseDriver.class == ns.getDatabaseDriver().getClass() && msg.getPeriod() == null) {
            throw new ApiMessageInterceptionException(argerr("Period field can not be null for metric [name:%s]", msg.getMetricName()));
        } else if (MysqlDatabaseDriver.class == ns.getDatabaseDriver().getClass()) {
            if (msg.getPeriod() != null) {
                throw new ApiMessageInterceptionException(argerr("Period field is not supported for metric [name:%s]", msg.getMetricName()));
            } else {
                msg.setPeriod(0);
            }
        }

        Optional<Metric> opt = ns.getMetrics().stream().filter(m->m.getName().equals(msg.getMetricName())).findAny();
        if (!opt.isPresent()) {
            throw new ApiMessageInterceptionException(argerr("namespace[%s] doesn't have the metric[%s]", msg.getNamespace(), msg.getMetricName()));
        }

        Metric metric = opt.get();

        if (metric.isAdminOnly() && !Account.isAdminPermission(msg.getSession())) {
            throw new ApiMessageInterceptionException(argerr("the metric[%s] is admin only, not available for current user", msg.getMetricName()));
        }

        if (msg.getLabels() != null) {
            Map<String, List> dup = new HashMap<>();
            List<String> allowedLabels = ns.getEffectiveMetricLabelNames(metric);

            msg.getLabels().forEach(l -> {
                if (!allowedLabels.contains(l.getKey())) {
                    throw new ApiMessageInterceptionException(argerr("the metric[%s] doesn't have the label[%s]", msg.getMetricName(), l.getKey()));
                }

                l.check();

                List lst = dup.computeIfAbsent(l.getKey(), x->new ArrayList());
                lst.add(l.getValue());
            });

            dup.forEach((k, l)-> {
                if (l.size() > 1) {
                    throw new ApiMessageInterceptionException(argerr("duplicate key[%s] with values%s", k, l));
                }
            });
        }

        if (msg.getActions() != null) {
            msg.getActions().forEach(a -> checkAction(a.actionUuid, a.actionType));
        }
    }

    private void validate(APIUpdateAlarmMsg msg) {
        if (msg.getActions() != null) {
            msg.getActions().forEach(a -> checkAction(a.actionUuid, a.actionType));
        }
    }

    private void checkAction(String actionUuid, String actionType) {
        if (!AlarmActionType.getAllTypes().containsKey(actionType)) {
            throw new ApiMessageInterceptionException(argerr("invalid action type[%s]", actionType));
        }

        AlarmActionFactory f = alarmManager.getAlarmActionFactory(actionType);
        if (!f.isActionExists(actionUuid)) {
            throw new ApiMessageInterceptionException(argerr("action[uuid:%s, type:%s] not found", actionUuid, actionType));
        }
    }

    private void validate(APIAddActionToAlarmMsg msg) {
        if (Q.New(AlarmActionVO.class).eq(AlarmActionVO_.alarmUuid, msg.getAlarmUuid())
                .eq(AlarmActionVO_.actionUuid, msg.getActionUuid())
                .eq(AlarmActionVO_.actionType, msg.getActionType()).isExists()) {
            throw new OperationFailureException(operr("duplicated action[uuid:%s, type:%s] for the alarm[uuid:%s]",
                    msg.getActionUuid(), msg.getActionType(), msg.getAlarmUuid()));
        }

        checkAction(msg.getActionUuid(), msg.getActionType());
    }

    private void validate(APIAddLabelToAlarmMsg msg) {
        if (Q.New(AlarmLabelVO.class).eq(AlarmLabelVO_.alarmUuid, msg.getAlarmUuid())
                .eq(AlarmLabelVO_.key, msg.getKey()).isExists()) {
            throw new OperationFailureException(argerr("duplicate label[key:%s, operator:%s, value:%s] for the alarm[uuid:%s]",
                    msg.getKey(), msg.getOperator(), msg.getValue(), msg.getAlarmUuid()));
        }
    }

    private void validate(APIRemoveLabelFromAlarmMsg msg) {
        String alarmUuid = Q.New(AlarmLabelVO.class).select(AlarmLabelVO_.alarmUuid).eq(AlarmLabelVO_.uuid, msg.getUuid()).findValue();
        msg.setAlarmUuid(alarmUuid);
    }

    private void validate(APICreateMonitorGroupMsg msg) {
        if (msg.getActions() != null) {
            msg.getActions().forEach(a -> checkAction(a.actionUuid, a.actionType));
        }
    }

    private void validate(APIUpdateMonitorGroupMsg msg) {
        if (msg.getActions() != null) {
            msg.getActions().forEach(a -> checkAction(a.actionUuid, a.actionType));
        }
    }

    private void validate(APIAddMetricRuleTemplateMsg msg) {
        APICreateAlarmMsg createAlarmMsg = new APICreateAlarmMsg();
        createAlarmMsg.setName(msg.getName());
        createAlarmMsg.setType(AlarmType.Any.toString());
        createAlarmMsg.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
        createAlarmMsg.setEmergencyLevel(msg.getEmergencyLevel());
        createAlarmMsg.setLabels(msg.getLabels());
        createAlarmMsg.setMetricName(msg.getMetricName());
        createAlarmMsg.setNamespace(msg.getNamespace());
        createAlarmMsg.setPeriod(msg.getPeriod());
        createAlarmMsg.setRepeatCount(msg.getRepeatCount());
        createAlarmMsg.setPeriod(msg.getPeriod());
        createAlarmMsg.setRepeatInterval(msg.getRepeatInterval());
        createAlarmMsg.setThreshold(msg.getThreshold());
        validate(createAlarmMsg);
    }

    private void validate(APIUpdateMetricRuleTemplateMsg msg) {
        MetricRuleTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), MetricRuleTemplateVO.class);

        Namespace ns = Namespace.getMetricNameSpace(templateVO.getNamespace(), templateVO.getMetricName());
        Optional<Metric> opt = ns.getMetrics().stream().filter(m->m.getName().equals(templateVO.getMetricName())).findAny();
        if (!opt.isPresent()) {
            throw new ApiMessageInterceptionException(argerr("namespace[%s] doesn't have the metric[%s]", templateVO.getNamespace(), templateVO.getMetricName()));
        }
        Metric metric = opt.get();

        if (msg.getLabels() != null) {
            Map<String, List> dup = new HashMap<>();
            List<String> allowedLabels = ns.getEffectiveMetricLabelNames(metric);

            msg.getLabels().forEach(l -> {
                if (!allowedLabels.contains(l.getKey())) {
                    throw new ApiMessageInterceptionException(argerr("the metric[%s] doesn't have the label[%s]", templateVO.getMetricName(), l.getKey()));
                }

                l.check();

                List lst = dup.computeIfAbsent(l.getKey(), x->new ArrayList());
                lst.add(l.getValue());
            });

            dup.forEach((k, l)-> {
                if (l.size() > 1) {
                    throw new ApiMessageInterceptionException(argerr("duplicate key[%s] with values%s", k, l));
                }
            });
        }
    }

    private void validate(APIAddEventRuleTemplateMsg msg) {
        APISubscribeEventMsg subscribeEventMsg = new APISubscribeEventMsg();
        subscribeEventMsg.setNamespace(msg.getNamespace());
        subscribeEventMsg.setEventName(msg.getEventName());
        subscribeEventMsg.setLabels(msg.getLabels());
        validate(subscribeEventMsg);
    }

    private void validate(APIUpdateEventRuleTemplateMsg msg) {
        EventRuleTemplateVO templateVO = dbf.findByUuid(msg.getUuid(), EventRuleTemplateVO.class);

        APISubscribeEventMsg subscribeEventMsg = new APISubscribeEventMsg();
        subscribeEventMsg.setNamespace(templateVO.getNamespace());
        subscribeEventMsg.setEventName(templateVO.getEventName());
        subscribeEventMsg.setLabels(msg.getLabels());
        validate(subscribeEventMsg);
    }

    private void validate(APIChangeActiveAlarmStateMsg msg) {
        boolean exists = Q.New(ActiveAlarmTemplateVO.class)
               .eq(ActiveAlarmTemplateVO_.namespace, msg.getNamespace())
               .isExists();
        if (!exists) {
           throw new ApiMessageInterceptionException(argerr("namespace[%s] not support", msg.getNamespace()));
        }

        List<String> metricNames = Q.New(ActiveAlarmTemplateVO.class)
                .select(ActiveAlarmTemplateVO_.metricName)
                .eq(ActiveAlarmTemplateVO_.namespace, msg.getNamespace())
                .listValues();
        for (String metricName : metricNames) {
            Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), metricName);
            Optional<Metric> opt = ns.getMetrics().stream().filter(m -> m.getName().equals(metricName)).findAny();
            if (!opt.isPresent()) {
                throw new ApiMessageInterceptionException(argerr("namespace[%s] doesn't have the metric[%s]", msg.getNamespace(), metricName));
            }
            Metric metric = opt.get();
            if (metric.isAdminOnly() && !Account.isAdminPermission(msg.getSession())) {
                throw new ApiMessageInterceptionException(argerr("the metric[%s] is admin only, not available for current user", metric));
            }
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIDeleteAlarmMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
