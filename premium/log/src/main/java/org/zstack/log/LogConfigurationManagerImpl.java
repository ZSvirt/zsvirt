package org.zstack.log;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.core.log.LogGlobalConfig;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.identity.Account;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.log.server.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;


@InterceptorForService(LogConfigurationConstant.SERVICE_ID)
public class LogConfigurationManagerImpl extends AbstractService implements LogConfigurationManager,
        ManagementNodeReadyExtensionPoint, ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(LogConfigurationManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CascadeFacade casf;

    private Map<String, LogConfigurationFactory> logConfigurationFactories = Collections.synchronizedMap(new HashMap<>());
    private Future<Void> syncCustomLogConfigurationTask;

    public LogConfigurationFactory getLogConfigurationFactory(String type) {
        return logConfigurationFactories.get(type);
    }

    public void handleMessage(Message msg) {
        if (msg instanceof LogServerMessage) {
            passThrough((LogServerMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(LogServerMessage msg) {
        LogServerVO vo = dbf.findByUuid(msg.getLogServerUuid(), LogServerVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find logServer[uuid=%s]", msg.getLogServerUuid()));
            return;
        }

        LogServerBase base = new LogServerBase(vo, bus, dbf, thdf, casf, this);
        base.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ApplyLogConfigurationMsg) {
            handle((ApplyLogConfigurationMsg) msg);
        } else if (msg instanceof DeleteLogConfigurationMsg) {
            handle((DeleteLogConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteLogConfigurationMsg msg) {
        deleteLogConfigurationById(msg.getConfigId());
        bus.reply(msg, new DeleteLogConfigurationReply());
    }

    private void deleteLogConfigurationById(long configId) {
        JsonLabelVO vo = Q.New(JsonLabelVO.class)
                .eq(JsonLabelVO_.resourceUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .eq(JsonLabelVO_.id, configId)
                .find();

        LogConfigurationStruct struct = JSONObjectUtil.toObject(vo.getLabelValue(), LogConfigurationStruct.class);
        doDeleteLogConfiguration(struct.getUuid(), vo.getLabelKey().split("-")[0]);
    }

    private void doDeleteLogConfiguration(String uuid, String type) {
        logConfigurationFactories.get(type).deleteLogConfiguration(uuid);
    }

    private void handle(ApplyLogConfigurationMsg msg) {
        AddLogConfigurationReply reply = new AddLogConfigurationReply();

        applyLogConfiguration(msg.getStruct(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String value) {
                reply.setValue(LogServerInventory.maskSensitiveConfiguration(value));
                reply.setManagementNodeUuid(Platform.getManagementServerId());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddLogConfigurationMsg) {
            handle((APIAddLogConfigurationMsg) msg);
        } else if (msg instanceof APIDeleteLogConfigurationMsg) {
            handle((APIDeleteLogConfigurationMsg) msg);
        } else if (msg instanceof APIUpdateLogConfigurationMsg) {
            handle((APIUpdateLogConfigurationMsg) msg);
        } else if (msg instanceof APIGetLogConfigurationMsg) {
            handle((APIGetLogConfigurationMsg) msg);
        } else if (msg instanceof APIAddLogServerMsg) {
            handle((APIAddLogServerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIAddLogServerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                // per-type lock: add Log4j2 and add FluentBit can run concurrently; add vs update/delete on same type cannot
                return String.format("%s-%s", LogServerConstant.OPERATE_LOG_SERVER_NAME, msg.getType());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIAddLogServerEvent evt = new APIAddLogServerEvent(msg.getId());

                doAddLogServer(msg, new ReturnValueCompletion<LogServerInventory>(msg, chain) {
                    @Override
                    public void success(LogServerInventory inv) {
                        evt.setInventory(inv);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doAddLogServer(APIAddLogServerMsg msg, ReturnValueCompletion<LogServerInventory> completion) {
        LogServerVO vo = new LogServerVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setCategory(LogCategory.valueOf(msg.getCategory()));
        vo.setType(LogType.valueOf(msg.getType()));
        if (StringUtils.isNotEmpty(msg.getLevel())) {
            vo.setLevel(LogLevel.valueOf(msg.getLevel()));
        }
        vo.setConfiguration(msg.getConfiguration());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        dbf.persist(vo);

        applyLogServerConfigurationToAllNodes(LogServerInventory.rawValueOf(vo), new ReturnValueCompletion<List<ApplyLogConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyLogConfigurationResult> returnValue) {
                LogServerVO updated = dbf.findByUuid(vo.getUuid(), LogServerVO.class);
                completion.success(LogServerInventory.valueOf(updated == null ? vo : updated));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(vo);
                completion.fail(errorCode);
            }
        });
    }

    private void applyLogServerConfigurationToAllNodes(LogServerInventory inv, ReturnValueCompletion<List<ApplyLogConfigurationResult>> completion) {
        if (inv.getState() == LogServerState.Disabled) {
            completion.success(Collections.emptyList());
            return;
        }
        LogConfigurationStruct struct = new LogConfigurationStruct();
        struct.setUuid(inv.getUuid());
        struct.setName(inv.getName());
        struct.setDescription(inv.getDescription());
        struct.setAccountUuid(inv.getAccountUuid());
        struct.setCategory(inv.getCategory().toString());
        struct.setType(inv.getType().toString().toLowerCase());
        if (inv.getLevel() != null) {
            struct.setLevel(inv.getLevel().toString());
        }
        struct.setConfiguration(inv.getConfiguration());

        LogConfigurationFactory factory = logConfigurationFactories.get(struct.getType());
        if (factory == null) {
            throw new OperationFailureException(operr("No factory found for type:%s", struct.getType()));
        }

        final List<ApplyLogConfigurationResult> results = Collections.synchronizedList(new ArrayList<>());
        FlowChain chain = new SimpleFlowChain();
        chain.setName("apply-log-server-configuration-to-all-nodes");
        chain.then(new Flow() {
            String __name__ = "apply-log-server-configurations";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();

                final ErrorCode[] errorCode = new ErrorCode[1];
                new While<>(mnUuids).each((mnUuid, whileCompletion) -> {
                    ApplyLogServerConfigurationMsg amsg = new ApplyLogServerConfigurationMsg();
                    amsg.setStruct(struct);
                    bus.makeServiceIdByManagementNodeId(amsg, LogConfigurationConstant.SERVICE_ID, mnUuid);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            ApplyLogConfigurationResult result = new ApplyLogConfigurationResult();
                            result.setManagementNodeUuid(mnUuid);
                            results.add(result);

                            if (!reply.isSuccess()) {
                                result.setErrorCode(reply.getError());
                                errorCode[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCode[0] != null) {
                            trigger.fail(errorCode[0]);
                            return;
                        }

                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (results.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                logger.debug("delete log server configurations no succeed nodes");

                new While<>(results).each((result, whileCompletion) -> {
                    DeleteLogServerConfigurationMsg dmsg = new DeleteLogServerConfigurationMsg();
                    dmsg.setUuid(inv.getUuid());
                    bus.makeServiceIdByManagementNodeId(dmsg, LogConfigurationConstant.SERVICE_ID, result.getManagementNodeUuid());
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to roll back log server[uuid:%s] configuration on node[uuid:%s]: %s",
                                        inv.getUuid(), result.getManagementNodeUuid(), reply.getError()));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(results);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIGetLogConfigurationMsg msg) {
        APIGetLogConfigurationReply reply = new APIGetLogConfigurationReply();
        List<JsonLabelInventory> invs = new ArrayList<>();

        for (String type : logConfigurationFactories.keySet()) {
            List<JsonLabelVO> vos = Q.New(JsonLabelVO.class)
                    .like(JsonLabelVO_.labelKey, type + "-%")
                    .list();
            for (JsonLabelVO vo : vos) {
                invs.add(JsonLabelInventory.valueOf(vo));
            }
        }

        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateLogConfigurationMsg msg) {
        APIUpdateLogConfigurationEvent evt = new APIUpdateLogConfigurationEvent(msg.getId());
        JsonLabelVO vo = dbf.findById(msg.getConfigId(), JsonLabelVO.class);
        LogConfigurationStruct struct = JSONObjectUtil.toObject(vo.getLabelValue(), LogConfigurationStruct.class);

        if (msg.getName() != null) {
            struct.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            struct.setDescription(msg.getDescription());
        }

        vo.setLabelValue(JSONObjectUtil.toJsonString(struct));
        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(JsonLabelInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteLogConfigurationMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("log-configuration-%d", msg.getConfigId());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteLogConfigurationEvent evt = new APIDeleteLogConfigurationEvent(msg.getId());

                List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();

                new While<>(mnUuids).each((mnUuid, completion) -> {
                    DeleteLogConfigurationMsg dmsg = new DeleteLogConfigurationMsg();
                    dmsg.setConfigId(msg.getConfigId());
                    bus.makeServiceIdByManagementNodeId(dmsg, LogConfigurationConstant.SERVICE_ID, mnUuid);
                    bus.send(dmsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                completion.addError(reply.getError());
                                completion.done();
                                return;
                            }

                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        // if any error happened, remain the db record for next effort to delete the config
                        if (!errorCodeList.getCauses().isEmpty()) {
                            bus.publish(evt);
                            chain.next();
                            return;
                        }

                        SQL.New(JsonLabelVO.class)
                                .eq(JsonLabelVO_.id, msg.getConfigId())
                                .eq(JsonLabelVO_.resourceUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                                .delete();
                        bus.publish(evt);
                        chain.next();
                    }
                });

            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private String makeLabelKey(String type, String uuid) {
        return String.format("%s-%s", type, uuid);
    }

    private void applyLogConfiguration(LogConfigurationStruct struct, ReturnValueCompletion<String> completion) {
        logConfigurationFactories.get(struct.getType()).createLogConfiguration(struct, new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String value) {
                completion.success(value);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyLogConfigurationToAllNodes(JsonLabelInventory inv, ReturnValueCompletion<List<ApplyLogConfigurationResult>> completion) {
        LogConfigurationStruct struct = JSONObjectUtil.toObject(inv.getLabelValue(), LogConfigurationStruct.class);

        LogConfigurationFactory factory = logConfigurationFactories.get(struct.getType());
        if (factory == null) {
            throw new OperationFailureException(operr("No factory found for type:%s", struct.getType()));
        }

        final List<ApplyLogConfigurationResult> results = Collections.synchronizedList(new ArrayList<>());
        FlowChain chain = new SimpleFlowChain();
        chain.setName("apply-configuration-to-all-nodes");
        chain.then(new Flow() {
            String __name__ = "apply-configurations";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();

                final ErrorCode[] errorCode = new ErrorCode[1];
                new While<>(mnUuids).each((mnUuid, whileCompletion) -> {
                    ApplyLogConfigurationMsg amsg = new ApplyLogConfigurationMsg();
                    amsg.setStruct(struct);
                    amsg.setLabelKey(makeLabelKey(struct.getType(), struct.getUuid()));
                    bus.makeServiceIdByManagementNodeId(amsg, LogConfigurationConstant.SERVICE_ID, mnUuid);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            ApplyLogConfigurationResult result = new ApplyLogConfigurationResult();
                            result.setManagementNodeUuid(mnUuid);
                            results.add(result);

                            if (!reply.isSuccess()) {
                                result.setErrorCode(reply.getError());
                                errorCode[0] = reply.getError();
                                whileCompletion.allDone();
                                return;
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCode[0] != null) {
                            trigger.fail(errorCode[0]);
                            return;
                        }

                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (results.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                logger.debug("delete log configurations no succeed nodes");

                new While<>(results).each((result, whileCompletion) -> {
                    DeleteLogConfigurationMsg dmsg = new DeleteLogConfigurationMsg();
                    dmsg.setConfigId(inv.getId());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, LogConfigurationConstant.SERVICE_ID, result.getManagementNodeUuid());
                    bus.send(dmsg);
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(results);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void doAddLogConfiguration(APIAddLogConfigurationMsg msg, ReturnValueCompletion<JsonLabelInventory> completion) {
        LogConfigurationStruct struct = new LogConfigurationStruct();

        if (msg.getResourceUuid() != null) {
            struct.setUuid(msg.getResourceUuid());
        } else {
            struct.setUuid(Platform.getUuid());
        }

        struct.setName(msg.getName());
        struct.setDescription(msg.getDescription());
        struct.setType(msg.getType());
        struct.setLevel(msg.getLevel());
        struct.setConfiguration(msg.getConfiguration());
        JsonLabelInventory inv = new JsonLabel().create(makeLabelKey(struct.getType(), struct.getUuid()), JSONObjectUtil.toJsonString(struct), AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);

        applyLogConfigurationToAllNodes(inv, new ReturnValueCompletion<List<ApplyLogConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyLogConfigurationResult> returnValue) {
                completion.success(inv);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(APIAddLogConfigurationMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("add-log-configuration-%s", msg.getType());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIAddLogConfigurationEvent evt = new APIAddLogConfigurationEvent(msg.getId());

                doAddLogConfiguration(msg, new ReturnValueCompletion<JsonLabelInventory>(msg, chain) {
                    @Override
                    public void success(JsonLabelInventory inv) {
                        evt.setInventory(inv);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    public String getId() {
        return bus.makeLocalServiceId(LogConfigurationConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (LogConfigurationFactory f : pluginRgty.getExtensionList(LogConfigurationFactory.class)) {
            String type = f.getLogConfigurationType();

            LogConfigurationFactory old = logConfigurationFactories.get(type);

            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LogConfigurationFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getLogConfigurationType()));
            }

            logConfigurationFactories.put(type, f);
        }
    }

    public boolean start() {
        populateExtensions();
        return true;
    }

    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        LogGlobalConfig.SYNC_CUSTOM_LOG_CONFIGURATION_TASK_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startSyncCustomLogConfigurationTask());
        startSyncCustomLogConfigurationTask();
    }

    private void startSyncCustomLogConfigurationTask() {
        if (syncCustomLogConfigurationTask != null) {
            syncCustomLogConfigurationTask.cancel(false);
        }

        syncCustomLogConfigurationTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return LogGlobalConfig.SYNC_CUSTOM_LOG_CONFIGURATION_TASK_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "sync-custom-log-configuration-task";
            }

            @Override
            public void run() {
                loadConfigurations();
            }
        });
    }

    private void loadConfigurations() {
        Map<String, Appender> appenderMap = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppenders();
        for (LogConfigurationFactory f : logConfigurationFactories.values()) {
            List<String> configUuids = Q.New(LogServerVO.class)
                    .select(LogServerVO_.uuid)
                    .eq(LogServerVO_.category, LogCategory.ManagementNodeLog)
                    .listValues();

            // load missing appenders
            boolean anyMissing = configUuids.stream().anyMatch(uuid -> !appenderMap.containsKey(uuid));
            if (anyMissing) {
                logger.info(String.format("current appender map: %s", appenderMap));
                f.loadConfiguration();
                logger.info(String.format("load after appender map: %s", ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppenders()));
            }
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLogConfigurationMsg) {
            validate((APIAddLogConfigurationMsg) msg);
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, ((APIAddLogConfigurationMsg) msg).getType());
        } else if (msg instanceof APIDeleteLogConfigurationMsg) {
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, ((APIDeleteLogConfigurationMsg) msg).getConfigId().toString());
        } else if (msg instanceof APIAddLogServerMsg) {
            validate((APIAddLogServerMsg) msg);
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, ((APIAddLogServerMsg) msg).getType());
        } else if (msg instanceof LogServerMessage) {
            if (msg instanceof APIUpdateLogServerMsg) {
                validate((APIUpdateLogServerMsg) msg);
            } else if (msg instanceof APIDeleteLogServerMsg) {
                validate((APIDeleteLogServerMsg) msg);
            }
            LogServerMessage lmsg = (LogServerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, lmsg.getLogServerUuid());
        }

        return msg;
    }

    private void validate(APIUpdateLogServerMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        checkPermissions(msg.getUuid(), accountUuid);
    }

    private void validate(APIDeleteLogServerMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        checkPermissions(msg.getUuid(), accountUuid);
    }

    private void checkPermissions(String logServerUuid, String accountUuid) {
        if (Account.isAdminPermission(accountUuid)) {
            return;
        }
        LogServerVO logServer = SQL.New("select vo from LogServerVO vo, AccountResourceRefVO ref " +
                        "where vo.uuid = ref.resourceUuid and ref.resourceUuid = :logServerUuid and ref.accountUuid = :auuid", LogServerVO.class)
                .param("logServerUuid", logServerUuid)
                .param("auuid", accountUuid).find();
        if (logServer == null) {
            throw new ApiMessageInterceptionException(argerr("the account[uuid:%s] is not authorized to " +
                    "operate on log server[uuid:%s]", accountUuid, logServerUuid));
        }
    }

    private void validate(APIAddLogServerMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        if (!Account.isAdminPermission(accountUuid)) {
            if (msg.getCategory().equals(LogCategory.ManagementNodeLog.toString())) {
                throw new ApiMessageInterceptionException(argerr("Access denied: Account[uuid:%s] is not authorized to get management node log." +
                        " Only admin accounts have this privilege.", accountUuid));
            }

            long count = SQL.New("select count(vo) from LogServerVO vo, AccountResourceRefVO ref " +
                            "where vo.uuid = ref.resourceUuid and ref.accountUuid = :auuid", Long.class)
                    .param("auuid", accountUuid).find();
            if (count > 0) {
                throw new OperationFailureException(operr("The account[uuid:%s] is not authorized to add more than one log server. " +
                        "Only admin accounts have this privilege.", accountUuid));
            }
        }

        if (Arrays.stream(LogCategory.values()).noneMatch(c -> c.toString().equals(msg.getCategory()))) {
            throw new ApiMessageInterceptionException(argerr("Unknown log server category %s", msg.getCategory()));
        }
        if (StringUtils.isNotEmpty(msg.getLevel())
                && Arrays.stream(LogLevel.values()).noneMatch(l -> l.toString().equals(msg.getLevel()))) {
            throw new ApiMessageInterceptionException(argerr("Unknown log level %s", msg.getLevel()));
        }

        LogConfigurationStruct struct = new LogConfigurationStruct();

        if (msg.getResourceUuid() != null) {
            struct.setUuid(msg.getResourceUuid());
        } else {
            struct.setUuid(Platform.getUuid());
        }

        struct.setName(msg.getName());
        struct.setDescription(msg.getDescription());
        struct.setCategory(msg.getCategory());
        struct.setType(msg.getType().toLowerCase());
        struct.setConfiguration(msg.getConfiguration());

        LogConfigurationFactory factory = logConfigurationFactories.get(struct.getType());

        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown log server configuration type %s", struct.getType()));
        }

        factory.validate(struct);
    }

    private void validate(APIAddLogConfigurationMsg msg) {
        LogConfigurationStruct struct = new LogConfigurationStruct();

        if (msg.getResourceUuid() != null) {
            struct.setUuid(msg.getResourceUuid());
        } else {
            struct.setUuid(Platform.getUuid());
        }

        struct.setName(msg.getName());
        struct.setDescription(msg.getDescription());
        struct.setType(msg.getType());
        struct.setConfiguration(msg.getConfiguration());

        LogConfigurationFactory factory = logConfigurationFactories.get(struct.getType());

        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown log configuration type %s", msg.getType()));
        }

        factory.validate(struct);
    }
}
