package org.zstack.zwatch.thirdparty;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.metricpusher.MetricTemplateUtils;
import org.zstack.zwatch.namespace.event.ThirdpartyAlertCanonicalEvents;
import org.zstack.zwatch.thirdparty.api.APIDeleteThirdpartyPlatformEvent;
import org.zstack.zwatch.thirdparty.api.APIDeleteThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyPlatformEvent;
import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.entity.*;
import org.zstack.zwatch.thirdparty.msg.DeleteThirdPartyPlatformMsg;
import org.zstack.zwatch.thirdparty.msg.DeleteThirdPartyPlatformReply;
import org.zstack.zwatch.thirdparty.msg.SyncThirdpartyAlertsMsg;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class ThirdpartyPlatformBase {
    protected static final CLogger logger = Utils.getLogger(ThirdpartyPlatformBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected EventFacade evtf;

    protected ThirdpartyPlatformVO self;
    protected ThirdpartyPlatformVO originalCopy;
    private String syncThreadName;

    protected ThirdpartyPlatformVO getSelf() {
        return self;
    }

    protected ThirdpartyPlatformInventory getSelfInventory() {
        return ThirdpartyPlatformInventory.valueOf(self);
    }

    public ThirdpartyPlatformBase() {
    }

    public ThirdpartyPlatformBase(ThirdpartyPlatformVO vo) {
        this.self = vo;
        this.syncThreadName = "thirdpartyPlatform-" + vo.getUuid();
        this.originalCopy = ObjectUtils.newAndCopy(vo, vo.getClass());
    }

    @MessageSafe
    public void handleMessage(final Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateThirdpartyPlatformMsg) {
            handle((APIUpdateThirdpartyPlatformMsg) msg);
        } else if (msg instanceof APIDeleteThirdpartyPlatformMsg) {
            handle((APIDeleteThirdpartyPlatformMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncThirdpartyAlertsMsg) {
            handle((SyncThirdpartyAlertsMsg) msg);
        } else if (msg instanceof DeleteThirdPartyPlatformMsg) {
            handle((DeleteThirdPartyPlatformMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateThirdpartyPlatformMsg msg) {
        APIUpdateThirdpartyPlatformEvent event = new APIUpdateThirdpartyPlatformEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                ThirdpartyPlatformVO vo = dbf.findByUuid(msg.getUuid(), ThirdpartyPlatformVO.class);

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getTemplate() != null) {
                    vo.setTemplate(msg.getTemplate());
                }

                if (msg.getUrl() != null) {
                    vo.setUrl(msg.getUrl());
                }

                String stateEvent = msg.getStateEvent();
                if (stateEvent != null) {
                    if (stateEvent.equals("enable")) {
                        vo.setState(ThirdpartyPlatformState.Enabled.name());
                    } else {
                        vo.setState(ThirdpartyPlatformState.Disabled.name());
                    }
                }

                if (msg.getLastSyncDateMills() != null) {
                    vo.setLastSyncDate(new Timestamp(msg.getLastSyncDateMills()));
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                vo = dbf.updateAndRefresh(vo);
                event.setInventory(ThirdpartyPlatformInventory.valueOf(vo));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("update-thirdparty-platform", msg.getUuid());
            }
        });
    }

    private void handle(APIDeleteThirdpartyPlatformMsg msg) {
        DeleteThirdPartyPlatformMsg innerMsg = DeleteThirdPartyPlatformMsg.valueOf(msg);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, Constants.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIDeleteThirdpartyPlatformEvent event = new APIDeleteThirdpartyPlatformEvent(msg.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(DeleteThirdPartyPlatformMsg msg) {
        DeleteThirdPartyPlatformReply reply = new DeleteThirdPartyPlatformReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                long count = Q.New(ThirdpartyOriginalAlertVO.class)
                        .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, msg.getUuid())
                        .count();
                SQL.New("select vo.uuid from ThirdpartyOriginalAlertVO vo where vo.thirdpartyPlatformUuid = :platformUuid")
                        .param("platformUuid", msg.getUuid())
                        .limit(1000)
                        .paginate(count, (List<String> alertUuids) -> {
                            SQL.New(SNSEndpointThirdpartyAlertHistoryVO.class)
                                    .in(SNSEndpointThirdpartyAlertHistoryVO_.alertUuid, alertUuids)
                                    .hardDelete();
                        });

                SQL.New(ThirdpartyOriginalAlertVO.class)
                        .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, msg.getUuid())
                        .hardDelete();

                dbf.remove(self);

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("delete-thirdparty-platform-%s", msg.getUuid());
            }
        });
    }

    private void handle(SyncThirdpartyAlertsMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                Date start = new Date(self.getLastSyncDate().getTime());
                Date end = new Date(System.currentTimeMillis());
                syncThirdpartyAlerts(start, end, new Completion(msg) {
                    @Override
                    public void success() {
                        SQL.New(ThirdpartyPlatformVO.class)
                                .eq(ThirdpartyPlatformVO_.uuid, self.getUuid())
                                .set(ThirdpartyPlatformVO_.lastSyncDate, new Timestamp(end.getTime()))
                                .update();

                        bus.reply(msg, new MessageReply());
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        saveSyncAlertsError(errorCode);

                        MessageReply reply = new MessageReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("sync-thirdparty-%s-alerts", self.getUuid());
            }
        });
    }

    private void saveSyncAlertsError(ErrorCode errorCode) {
        SystemTagCreator creator = ThirdpartyPlatformSystemTags.LAST_SYNC_THIRDPARTY_ALERT_ERROR.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(ThirdpartyPlatformSystemTags.LAST_SYNC_THIRDPARTY_ALERT_ERROR_TOKEN, errorCode.toString())));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }

    protected abstract void syncThirdpartyAlerts(Date start, Date end, Completion completion);

    protected abstract List<ThirdpartyOriginalAlertVO> convert(GetAlertsRsp rsp);

    protected void persistAndFireAlerts(GetAlertsRsp rsp) {
        List<ThirdpartyOriginalAlertVO> alertVOS = convert(rsp);
        if (alertVOS.isEmpty()) {
            return;
        }

        List<String> uuids = alertVOS.stream()
                .map(ThirdpartyOriginalAlertVO::getUuid).collect(Collectors.toList());

        boolean exist = Q.New(ThirdpartyOriginalAlertVO.class)
                .in(ThirdpartyOriginalAlertVO_.uuid, uuids)
                .isExists();
        if (exist) {
            List<String> existingUuids = Q.New(ThirdpartyOriginalAlertVO.class)
                    .select(ThirdpartyOriginalAlertVO_.uuid)
                    .in(ThirdpartyOriginalAlertVO_.uuid, uuids)
                    .listValues();
            alertVOS = alertVOS.stream().filter(vo -> !existingUuids.contains(vo.getUuid()))
                    .collect(Collectors.toList());
        }

        if (alertVOS.isEmpty()) {
            return;
        }

        dbf.persistCollection(alertVOS);

        fireCanonicalEvents(alertVOS);

        executeAlertRetention();
    }

    protected void executeAlertRetention() {
        int reservedAmount = ZWatchGlobalConfig.THIRDPARTY_ALERT_RETENTION_AMOUNT.value(Integer.class);

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            // delete in batches after the quantity exceeds 1000
            reservedAmount += 1000;
        }

        long count = Q.New(ThirdpartyOriginalAlertVO.class)
                .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, self.getUuid())
                .count();
        if (count <= reservedAmount) {
            return;
        }

        Timestamp timestamp = Q.New(ThirdpartyOriginalAlertVO.class)
                .select(ThirdpartyOriginalAlertVO_.alertTime)
                .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, self.getUuid())
                .orderBy(ThirdpartyOriginalAlertVO_.alertTime, SimpleQuery.Od.DESC)
                .start(reservedAmount)
                .limit(1)
                .findValue();

        do {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    List<String> alertUuids = q(ThirdpartyOriginalAlertVO.class)
                            .select(ThirdpartyOriginalAlertVO_.uuid)
                            .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, self.getUuid())
                            .lte(ThirdpartyOriginalAlertVO_.alertTime, timestamp)
                            .limit(1000)
                            .listValues();
                    if (alertUuids.isEmpty()) {
                        return;
                    }

                    sql(ThirdpartyOriginalAlertVO.class)
                            .in(ThirdpartyOriginalAlertVO_.uuid, alertUuids)
                            .hardDelete();

                    sql(SNSEndpointThirdpartyAlertHistoryVO.class)
                            .in(SNSEndpointThirdpartyAlertHistoryVO_.alertUuid, alertUuids)
                            .hardDelete();
                }
            }.execute();

            count = Q.New(ThirdpartyOriginalAlertVO.class)
                    .eq(ThirdpartyOriginalAlertVO_.thirdpartyPlatformUuid, self.getUuid())
                    .lte(ThirdpartyOriginalAlertVO_.alertTime, timestamp)
                    .count();
        } while (count > 0);
    }

    private void fireCanonicalEvents(List<ThirdpartyOriginalAlertVO> alertVOS) {
        long alarmTimePeriodMillis = 1000 * ZWatchGlobalConfig.THIRDPARTY_ALERT_ALARM_TIME_PERIOD.value(Long.class);
        long currentTimeMillis = System.currentTimeMillis();
        for (ThirdpartyOriginalAlertVO vo : alertVOS) {
            if (Math.abs(currentTimeMillis - vo.getAlertTime().getTime()) > alarmTimePeriodMillis) {
                continue;
            }

            fireCanonicalEvent(vo);
        }
    }

    @SyncThread(signature="fire-thirdparty-alert-event")
    private void fireCanonicalEvent(ThirdpartyOriginalAlertVO vo) {
        ThirdpartyAlertCanonicalEvents.ThirdpartyAlertData data = new ThirdpartyAlertCanonicalEvents.ThirdpartyAlertData();
        data.setUuid(vo.getUuid());
        data.setPlatformUuid(vo.getThirdpartyPlatformUuid());
        data.setProduct(vo.getProduct());
        data.setService(vo.getService());
        data.setMetric(vo.getMetric());
        data.setAlertLevel(vo.getAlertLevel());
        data.setAlertTime(vo.getAlertTime().toString());
        data.setDimensions(vo.getDimensions());
        data.setMessage(vo.getMessage());
        data.setDataSource(vo.getDataSource());
        evtf.fire(ThirdpartyAlertCanonicalEvents.THIRDPARTY_ALERT_PATH, data);
    }

    protected ThirdpartyOriginalAlertVO render(Map alertMap) {
        String result = MetricTemplateUtils.render(self.getTemplate(), alertMap);
        ThirdpartyOriginalAlertVO alertVO = JSONObjectUtil.toObject(result, ThirdpartyOriginalAlertVO.class);
        alertVO.setUuid(Platform.getUuid());
        alertVO.setSourceText(JSONObjectUtil.toJsonString(alertMap));
        alertVO.setThirdpartyPlatformUuid(self.getUuid());
        alertVO.setReadStatus(ZWatchConstants.DATA_READ_STATUS_UNREAD);
        return alertVO;
    }
}
