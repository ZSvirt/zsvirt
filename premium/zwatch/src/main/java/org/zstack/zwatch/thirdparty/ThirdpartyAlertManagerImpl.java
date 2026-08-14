package org.zstack.zwatch.thirdparty;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.sns.SNSPublishExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.alarm.EventSubscriptionVO;
import org.zstack.zwatch.alarm.EventSubscriptionVO_;
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate;
import org.zstack.zwatch.namespace.ThirdpartyAlertNamespace;
import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformEvent;
import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyAlertsEvent;
import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyAlertsMsg;
import org.zstack.zwatch.thirdparty.entity.SNSEndpointThirdpartyAlertHistoryVO;
import org.zstack.zwatch.thirdparty.entity.SNSEndpointThirdpartyAlertHistoryVO_;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertVO;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertVO_;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformState;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO_;
import org.zstack.zwatch.thirdparty.msg.SyncThirdpartyAlertsMsg;
import org.zstack.zwatch.thirdparty.msg.ThirdpartyPlatformMsg;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThirdpartyAlertManagerImpl extends AbstractService
        implements ThirdpartyAlertManager, ManagementNodeReadyExtensionPoint, SNSPublishExtension {
    protected static final CLogger logger = Utils.getLogger(ThirdpartyAlertManagerImpl.class);

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

    private Map<String, ThirdpartyFactory> thirdpartyFactories = new HashMap<>();

    private Future syncAlertTask;
    private boolean synchronizing = false;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ThirdpartyPlatformMsg) {
            passThrough((ThirdpartyPlatformMsg) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddThirdpartyPlatformMsg) {
            handle((APIAddThirdpartyPlatformMsg) msg);
        } else if (msg instanceof APIUpdateThirdpartyAlertsMsg) {
            handle((APIUpdateThirdpartyAlertsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(ThirdpartyPlatformMsg msg) {
        String platformUuid = msg.getThirdpartyPlatformUuid();

        boolean exist = Q.New(ThirdpartyPlatformVO.class)
                .eq(ThirdpartyPlatformVO_.uuid, platformUuid)
                .isExists();
        if (!exist) {
            String err = String.format("Cannot find ThirdpartyPlatformVO[uuid:%s], it may have been deleted", platformUuid);
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        String type = Q.New(ThirdpartyPlatformVO.class)
                .eq(ThirdpartyPlatformVO_.uuid, platformUuid)
                .select(ThirdpartyPlatformVO_.type)
                .findValue();
        ThirdpartyFactory factory = getThirdpartyFactory(type);
        ThirdpartyPlatformBase thirdpartyPlatformBase = factory.getThirdpartyPlatformBase(platformUuid);
        thirdpartyPlatformBase.handleMessage((Message) msg);
    }

    public ThirdpartyFactory getThirdpartyFactory(String type) {
        ThirdpartyFactory f = thirdpartyFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find the MediaFactory[type:%s]", type));
        }
        return f;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(Constants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();

        ZWatchGlobalConfig.THIRDPARTY_ALERT_ENABLE.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (newConfig.value(Boolean.class)) {
                    startSyncAlertTask();
                } else {
                    cancelSyncAlertTask();
                }
            }
        });

        ZWatchGlobalConfig.THIRDPARTY_ALERT_PULL_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startSyncAlertTask());

        return true;
    }

    @Override
    public boolean stop() {
        cancelSyncAlertTask();
        return true;
    }

    @Override
    public void managementNodeReady() {
        if (ZWatchGlobalConfig.THIRDPARTY_ALERT_ENABLE.value(Boolean.class)) {
            startSyncAlertTask();
        }
    }

    private synchronized void startSyncAlertTask() {
        if (syncAlertTask != null) {
            syncAlertTask.cancel(true);
        }

        syncAlertTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ZWatchGlobalConfig.THIRDPARTY_ALERT_PULL_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "push-metric-data-to-receiver";
            }

            @Override
            public void run() {
                if (synchronizing) {
                    logger.warn("Currently syncing, skip this sync");
                    return;
                }

                synchronizing = true;
                syncThirdpartyPlatformAlerts(new NoErrorCompletion() {
                    @Override
                    public void done() {
                        synchronizing = false;
                    }
                });
            }
        });
    }

    private void cancelSyncAlertTask() {
        if (syncAlertTask != null) {
            syncAlertTask.cancel(true);
        }
    }

    private void populateExtensions() {
        for (ThirdpartyFactory f : pluginRgty.getExtensionList(ThirdpartyFactory.class)) {
            ThirdpartyFactory old = thirdpartyFactories.get(f.getThirdpartyType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ThirdpartyFactory[%s, %s] with the same type[%s]",
                        old.getClass(), f.getClass(), f.getThirdpartyType()));
            }

            thirdpartyFactories.put(f.getThirdpartyType(), f);
        }
    }

    private void syncThirdpartyPlatformAlerts(NoErrorCompletion completion) {
        List<String> uuids = Q.New(ThirdpartyPlatformVO.class)
                .select(ThirdpartyPlatformVO_.uuid)
                .eq(ThirdpartyPlatformVO_.state, ThirdpartyPlatformState.Enabled.name())
                .listValues();

        List<SyncThirdpartyAlertsMsg> msgs = new ArrayList<>();
        for (String uuid : uuids) {
            if (!destinationMaker.isManagedByUs(uuid)) {
                continue;
            }

            SyncThirdpartyAlertsMsg msg = new SyncThirdpartyAlertsMsg();
            msg.setPlatformUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, Constants.SERVICE_ID, uuid);
            msgs.add(msg);
        }

        if (msgs.isEmpty()) {
            synchronizing = false;
            completion.done();
            return;
        }

        new While<>(msgs).step((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void handle(APIAddThirdpartyPlatformMsg msg) {
        APIAddThirdpartyPlatformEvent event = new APIAddThirdpartyPlatformEvent(msg.getId());

        ThirdpartyPlatformVO vo = new ThirdpartyPlatformVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setType(msg.getType());
        vo.setTemplate(msg.getTemplate());
        vo.setName(msg.getName());
        vo.setState(ThirdpartyPlatformState.Enabled.name());
        vo.setUrl(msg.getUrl());
        vo.setLastSyncDate(new Timestamp(System.currentTimeMillis()));
        vo.setDescription(msg.getDescription());

        ThirdpartyFactory f = getThirdpartyFactory(msg.getType());
        vo = f.createMedia(vo, msg);
        vo = dbf.persist(vo);

        final ThirdpartyPlatformVO finalVO = vo;
        SyncThirdpartyAlertsMsg syncMsg = new SyncThirdpartyAlertsMsg();
        syncMsg.setPlatformUuid(vo.getUuid());
        bus.makeTargetServiceIdByResourceUuid(syncMsg, Constants.SERVICE_ID, vo.getUuid());
        bus.send(syncMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    dbf.remove(finalVO);
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                event.setInventory(f.getMediaInventory(finalVO));
                bus.publish(event);
            }
        });


    }

    private void handle(APIUpdateThirdpartyAlertsMsg msg) {
        APIUpdateThirdpartyAlertsEvent event = new APIUpdateThirdpartyAlertsEvent(msg.getId());

        String syncSignature;
        if (msg.getUuid() != null) {
            syncSignature = String.format("update-thirdparty-alert-%s", msg.getUuid());
        } else {
            syncSignature = "update-thirdparty-alerts";
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                UpdateQuery updateQuery = SQL.New(ThirdpartyOriginalAlertVO.class);

                if (msg.getUuid() != null) {
                    updateQuery.eq(ThirdpartyOriginalAlertVO_.uuid, msg.getUuid());
                }
                if (msg.getStartTimeMillis() != null) {
                    updateQuery.gte(ThirdpartyOriginalAlertVO_.createDate,
                            new Timestamp(msg.getStartTimeMillis()));
                }
                if (msg.getEndTimeMillis() != null) {
                    updateQuery.lte(ThirdpartyOriginalAlertVO_.createDate,
                            new Timestamp(msg.getEndTimeMillis()));
                }

                boolean flag = false;
                if (msg.getUpdateReadStatus() != null) {
                    updateQuery.set(ThirdpartyOriginalAlertVO_.readStatus,
                            msg.getUpdateReadStatus());
                    flag = true;
                }

                if (flag) {
                    updateQuery.update();
                }

                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-thirdparty-alert";
            }
        });
    }

    @Override
    public void afterPublishToEndpoint(String endpointUuid, Map<String, String> message) {
        String content = message.get("SYSTEM_HTTP");
        if (content == null) {
            return;
        }

        Map params = JSONObjectUtil.toObject(content, Map.class);

        String subscriptionUuid = (String) params.get(AbstractTextTemplate.PARAM_EVENT_SUBSCRIPTION_UUID);
        if (subscriptionUuid == null) {
            return;
        }

        boolean exists = Q.New(EventSubscriptionVO.class)
                .eq(EventSubscriptionVO_.uuid, subscriptionUuid)
                .eq(EventSubscriptionVO_.eventName, ThirdpartyAlertNamespace.ThirdpartyAlert.getName())
                .isExists();
        if (!exists) {
            return;
        }

        Map labels = (Map) params.get(AbstractTextTemplate.PARAM_EVENT_LABELS);
        if (labels == null) {
            return;
        }

        String alertUuid = (String) labels.get(ThirdpartyAlertNamespace.EventLabelNames.Uuid.toString());
        if (alertUuid == null) {
            return;
        }

        exists = Q.New(SNSEndpointThirdpartyAlertHistoryVO.class)
                .eq(SNSEndpointThirdpartyAlertHistoryVO_.endpointUuid, endpointUuid)
                .eq(SNSEndpointThirdpartyAlertHistoryVO_.alertUuid, alertUuid)
                .isExists();
        if (exists) {
            return;
        }

        SNSEndpointThirdpartyAlertHistoryVO refVO = new SNSEndpointThirdpartyAlertHistoryVO();
        refVO.setEndpointUuid(endpointUuid);
        refVO.setAlertUuid(alertUuid);
        refVO.setSubscriptionUuid(subscriptionUuid);
        refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        dbf.persist(refVO);
    }
}
