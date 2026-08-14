package org.zstack.zwatch.alarm.activealarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.activealarm.api.*;
import org.zstack.zwatch.alarm.activealarm.entity.*;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.message.AlarmDeletionMsg;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.ArrayList;
import java.util.List;

public class ActiveAlarmManagerImpl extends AbstractService
        implements ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(ActiveAlarmManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager accountManager;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetActiveAlarmStatusMsg) {
            handle((APIGetActiveAlarmStatusMsg) msg);
        } else if (msg instanceof APIChangeActiveAlarmStateMsg) {
            handle((APIChangeActiveAlarmStateMsg) msg);
        } else if (msg instanceof APIUpdateActiveAlarmTemplateMsg) {
            handle((APIUpdateActiveAlarmTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateActiveAlarmTemplateMsg msg) {
        APIUpdateActiveAlarmTemplateEvent evt = new APIUpdateActiveAlarmTemplateEvent(msg.getId());
        ActiveAlarmTemplateVO activeAlarmTemplateVO = dbf.findByUuid(msg.getUuid(), ActiveAlarmTemplateVO.class);

        if (activeAlarmTemplateVO == null) {
            ErrorCode err = Platform.err(SysErrors.RESOURCE_NOT_FOUND, "cannot find ActiveAlarmTemplateVO[uuid:%s], it may have been deleted", msg.getUuid());
            throw new OperationFailureException(err);
        }

        if (msg.getAlarmName() != null) {
            activeAlarmTemplateVO.setAlarmName(msg.getAlarmName());
        }
        if (msg.getComparisonOperator() != null) {
            activeAlarmTemplateVO.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
        }
        if (msg.getPeriod() != null) {
            activeAlarmTemplateVO.setPeriod(msg.getPeriod());
        }
        if (msg.getRepeatInterval() != null) {
            activeAlarmTemplateVO.setRepeatInterval(msg.getRepeatInterval());
        }
        if (msg.getThreshold() != null) {
            activeAlarmTemplateVO.setThreshold(msg.getThreshold());
        }
        if (msg.getRepeatCount() != null) {
            activeAlarmTemplateVO.setRepeatCount(msg.getRepeatCount());
        }
        if (msg.getEmergencyLevel() != null) {
            activeAlarmTemplateVO.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        }
        if (msg.getLabels() != null) {
            activeAlarmTemplateVO.setLabels(msg.getLabels());
        }

        activeAlarmTemplateVO = dbf.updateAndRefresh(activeAlarmTemplateVO);
        evt.setInventory(activeAlarmTemplateVO.toInventory());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ActiveAlarmConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {

    }

    private void handle(APIGetActiveAlarmStatusMsg msg) {
        APIGetActiveAlarmStatusReply reply = new APIGetActiveAlarmStatusReply();
        List<ActiveAlarmStatus> statusList = new ArrayList<>();

        List<String> templateNamespaces = Q.New(ActiveAlarmTemplateVO.class)
                .select(ActiveAlarmTemplateVO_.namespace)
                .groupBy(ActiveAlarmTemplateVO_.namespace)
                .listValues();

        List<ActiveAlarmVO> alarms = ResourceHelper.findOwnResources(ActiveAlarmVO.class, msg.getAccountUuid());
        List<String> alarmUuids = CollectionUtils.transform(alarms, ActiveAlarmVO::getAlarmUuid);

        if (alarmUuids.isEmpty()) {
            for (String namespace : templateNamespaces) {
                ActiveAlarmStatus status = new ActiveAlarmStatus();
                status.setNamespace(namespace);
                status.setStatus(ActiveAlarmState.Disabled.name());
                statusList.add(status);
            }

            reply.setStatuses(statusList);
            bus.reply(msg, reply);
            return;
        }

        List<String> alarmNamespaces = Q.New(AlarmVO.class)
                .select(AlarmVO_.namespace)
                .in(AlarmVO_.uuid, alarmUuids)
                .listValues();
        for (String namespace : templateNamespaces) {
            ActiveAlarmStatus status = new ActiveAlarmStatus();
            status.setNamespace(namespace);
            if (alarmNamespaces.contains(namespace)) {
                status.setStatus(ActiveAlarmState.Enabled.name());
            } else {
                status.setStatus(ActiveAlarmState.Disabled.name());
            }

            statusList.add(status);
        }

        reply.setStatuses(statusList);
        bus.reply(msg, reply);
    }

    private void handle(APIChangeActiveAlarmStateMsg msg) {
        APIChangeActiveAlarmStateEvent event = new APIChangeActiveAlarmStateEvent(msg.getId());
        String accountUuid = msg.getSession().getAccountUuid();
        String namespace = msg.getNamespace();

        if (msg.getStateEvent().equals("enable")) {
            enableActiveAlarm(accountUuid, namespace, new Completion(msg) {
                @Override
                public void success() {
                    bus.publish(event);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    event.setError(errorCode);
                    bus.publish(event);
                }
            });
            return;
        }

        disableActiveAlarm(accountUuid, namespace, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void enableActiveAlarm(String accountUuid, String namespace, Completion completion) {
        List<ActiveAlarmVO> alarms = ResourceHelper.findOwnResources(ActiveAlarmVO.class, accountUuid,
                q -> q.eq(ActiveAlarmVO_.namespace, namespace));
        List<String> templateUuids = CollectionUtils.transform(alarms, ActiveAlarmVO::getTemplateUuid);

        List<String> tempUuids = Q.New(ActiveAlarmTemplateVO.class)
                .select(ActiveAlarmTemplateVO_.uuid)
                .eq(ActiveAlarmTemplateVO_.namespace, namespace)
                .listValues();
        if (templateUuids.containsAll(tempUuids)) {
            completion.success();
            return;
        }

        tempUuids.removeAll(templateUuids);

        List<ActiveAlarmTemplateVO> templateVOS = Q.New(ActiveAlarmTemplateVO.class)
                .in(ActiveAlarmTemplateVO_.uuid, tempUuids)
                .list();

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(templateVOS).each((templateVO, compl) -> {
            CreateAlarmMsg createAlarmMsg = new CreateAlarmMsg();
            createAlarmMsg.setUuid(Platform.getUuid());
            String alarmName = templateVO.getNamespace().replace(Namespace.ZSTACK_NAMESPACE_PREFIX, "");
            alarmName = String.format("Active-%s-%s", alarmName.replace("/", ""), templateVO.getMetricName());
            createAlarmMsg.setName(alarmName);
            createAlarmMsg.setAccountUuid(accountUuid);
            if (templateVO.getLabels() != null) {
                List<Label> labels = JSONObjectUtil.toCollection(templateVO.getLabels(), ArrayList.class, Label.class);
                createAlarmMsg.setLabels(labels);
            }
            // TODO
            //String labelKey = ResourceVOToNamespaceMappingUtils.getRestrictLabelNames(resourceType);
            //labels.add(new Label(labelKey, Label.Operator.Regex, String.join("|", instanceUuids)));
            createAlarmMsg.setComparisonOperator(templateVO.getComparisonOperator());
            createAlarmMsg.setPeriod(templateVO.getPeriod());
            createAlarmMsg.setEmergencyLevel(templateVO.getEmergencyLevel().name());
            createAlarmMsg.setNamespace(templateVO.getNamespace());
            createAlarmMsg.setMetricName(templateVO.getMetricName());
            createAlarmMsg.setThreshold(templateVO.getThreshold());
            createAlarmMsg.setRepeatInterval(templateVO.getRepeatInterval());
            createAlarmMsg.setRepeatCount(templateVO.getRepeatCount());
            createAlarmMsg.setType(AlarmType.Any.name());
            bus.makeLocalServiceId(createAlarmMsg, AlarmConstants.SERVICE_ID);
            bus.send(createAlarmMsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                    } else {
                        CreateAlarmReply rly = (CreateAlarmReply) reply;
                        String alarmUuid = rly.getInventory().getUuid();
                        ActiveAlarmVO activeAlarmVO = new ActiveAlarmVO();
                        activeAlarmVO.setUuid(Platform.getUuid());
                        activeAlarmVO.setAccountUuid(accountUuid);
                        activeAlarmVO.setAlarmUuid(alarmUuid);
                        activeAlarmVO.setNamespace(templateVO.getNamespace());
                        activeAlarmVO.setTemplateUuid(templateVO.getUuid());
                        dbf.persist(activeAlarmVO);
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errs.get(0));
                }
            }
        });
    }

    private void disableActiveAlarm(String accountUuid, String namespace, Completion completion) {
        List<ActiveAlarmVO> alarms = ResourceHelper.findOwnResources(ActiveAlarmVO.class, accountUuid,
                q -> q.eq(ActiveAlarmVO_.namespace, namespace));
        List<String> alarmUuids = CollectionUtils.transform(alarms, ActiveAlarmVO::getAlarmUuid);

        if (alarmUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<String> existingAlarmUuids = Q.New(AlarmVO.class)
                .select(AlarmVO_.uuid)
                .in(AlarmVO_.uuid, alarmUuids)
                .listValues();
        List<String> deletedAlarmUuids = new ArrayList<>();
        deletedAlarmUuids.addAll(alarmUuids);
        deletedAlarmUuids.removeAll(existingAlarmUuids);
        if (!deletedAlarmUuids.isEmpty()) {
            SQL.New(ActiveAlarmVO.class)
                    .in(ActiveAlarmVO_.alarmUuid, deletedAlarmUuids)
                    .hardDelete();
        }

        if (existingAlarmUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(existingAlarmUuids).each((alarmUuid, compl) -> {
            AlarmDeletionMsg deleteAlarmMsg = new AlarmDeletionMsg();
            deleteAlarmMsg.setUuid(alarmUuid);
            bus.makeTargetServiceIdByResourceUuid(deleteAlarmMsg, AlarmConstants.SERVICE_ID, alarmUuid);
            bus.send(deleteAlarmMsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                    } else {
                        SQL.New(ActiveAlarmVO.class)
                                .eq(ActiveAlarmVO_.alarmUuid, alarmUuid)
                                .hardDelete();
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.isEmpty()) {
                    completion.fail(errs.get(0));
                    return;
                }

                completion.success();
            }
        });
    }
}