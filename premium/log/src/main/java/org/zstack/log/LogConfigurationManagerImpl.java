package org.zstack.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
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
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
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
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

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

    private Map<String, LogConfigurationFactory> logConfigurationFactories = Collections.synchronizedMap(new HashMap<>());

    public LogConfigurationFactory getLogConfigurationFactory(String type) {
        return logConfigurationFactories.get(type);
    }

    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
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
                reply.setValue(value);
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetLogConfigurationMsg msg) {
        List<JsonLabelInventory> invs = new ArrayList<>();

        for (String type : logConfigurationFactories.keySet()) {
            List<JsonLabelVO> list = Q.New(JsonLabelVO.class)
                    .like(JsonLabelVO_.labelKey, type + "-%")
                    .list();
            invs.addAll(CollectionUtils.transform(list, JsonLabelInventory::valueOf));
        }

        Comparator<JsonLabelInventory> comparator = Comparator.comparingLong(JsonLabelInventory::getId).reversed();
        invs.sort(comparator);

        APIGetLogConfigurationReply reply = new APIGetLogConfigurationReply();
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
        loadConfigurations();
    }

    @AsyncThread
    private void loadConfigurations() {
        for (LogConfigurationFactory f : logConfigurationFactories.values()) {
            f.loadConfiguration();
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLogConfigurationMsg) {
            validate((APIAddLogConfigurationMsg) msg);
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, ((APIAddLogConfigurationMsg) msg).getType());
        } else if (msg instanceof APIDeleteLogConfigurationMsg) {
            bus.makeTargetServiceIdByResourceUuid(msg, LogConfigurationConstant.SERVICE_ID, ((APIDeleteLogConfigurationMsg) msg).getConfigId().toString());
        }

        return msg;
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
