package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkGetVniExtensionPoint;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.PingCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.PingRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.lifecycle.TrackVirtualRouterVmFlow;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterCreatePublicVipFlow;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;
import static org.zstack.network.service.virtualrouter.VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_JOB;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.ADDITIONAL_PUBLIC_NIC_MASK;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.GUEST_NIC_MASK;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouter extends ApplianceVmBase {
    private static final CLogger logger = Utils.getLogger(VirtualRouter.class);

    static {
        allowedOperations.addState(VmInstanceState.Running, APIReconnectVirtualRouterMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, APIProvisionVirtualRouterConfigMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, APIUpdateVirtualRouterMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, ReconnectVirtualRouterVmMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, ProvisionVirtualRouterConfigMsg.class.getName());
    }

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    protected VirutalRouterDefaultL3ConfigProxy defaultL3ConfigProxy;

    protected VirtualRouterVmInventory vr;

    public VirtualRouter(ApplianceVmVO vo) {
        super(vo);
    }

    public VirtualRouter(VirtualRouterVmVO vo) {
        super(vo);
        vr = new VirtualRouterVmInventory(vo);
    }

    @Override
    protected VmInstanceInventory getSelfInventory() {
        return VirtualRouterVmInventory.valueOf(getSelf());
    }

    @Override
    protected List<Flow> getPostCreateFlows() {
        return vrMgr.getPostCreateFlows();
    }

    @Override
    protected List<Flow> getPostStartFlows() {
        return vrMgr.getPostStartFlows();
    }

    @Override
    protected List<Flow> getPostStopFlows() {
        return vrMgr.getPostStopFlows();
    }

    @Override
    protected List<Flow> getPostRebootFlows() {
        return vrMgr.getPostRebootFlows();
    }

    @Override
    protected List<Flow> getPostDestroyFlows() {
        return vrMgr.getPostDestroyFlows();
    }

    @Override
    protected List<Flow> getPostMigrateFlows() {
        return vrMgr.getPostMigrateFlows();
    }

    protected FlowChain getReconnectChain() {
        return vrMgr.getReconnectFlowChain();
    }

    protected FlowChain getProvisionConfigChain() {
        return vrMgr.getProvisionConfigChain();
    }

    @Override
    protected List<Flow> createAfterConnectNewCreatedVirtualRouterFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.add(new TrackVirtualRouterVmFlow());
        return flows;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectVirtualRouterMsg) {
            handle((APIReconnectVirtualRouterMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterMsg) {
            handle((APIUpdateVirtualRouterMsg) msg);
        } else if (msg instanceof APIProvisionVirtualRouterConfigMsg) {
            handle((APIProvisionVirtualRouterConfigMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            handle((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            handle((APIDetachL3NetworkFromVmMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof VirtualRouterAsyncHttpCallMsg) {
            handle((VirtualRouterAsyncHttpCallMsg) msg);
        } else if (msg instanceof ReconnectVirtualRouterVmMsg) {
            handle((ReconnectVirtualRouterVmMsg) msg);
        } else if (msg instanceof PingVirtualRouterVmMsg) {
            handle((PingVirtualRouterVmMsg) msg);
        } else if (msg instanceof ProvisionVirtualRouterConfigMsg) {
            handle((ProvisionVirtualRouterConfigMsg) msg);
        } else if (msg instanceof VirtualRouterOverlayInnerMsg) {
            handle((VirtualRouterOverlayInnerMsg) msg);
        } else if (msg instanceof DetachNicFromVmMsg) {
            handle((DetachNicFromVmMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    void doPing(String vrUuid, ReturnValueCompletion<PingVirtualRouterVmReply> completion) {
        PingCmd cmd = new PingCmd();
        cmd.setUuid(vrUuid);
        restf.asyncJsonPost(buildUrl(vr.getManagementNic().getIp(), VirtualRouterConstant.VR_PING), cmd, new JsonAsyncRESTCallback<PingRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(PingRsp ret) {
                PingVirtualRouterVmReply reply = new PingVirtualRouterVmReply(self.getUuid());

                refreshVO();
                if (getSelf().getStatus() == ApplianceVmStatus.Connecting
                        || getSelf().getState() == VmInstanceState.Rebooting) {
                    reply.setDoReconnect(false);
                } else {
                    reply.setDoReconnect(true);
                }

                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to ping the virtual router vm[uuid:%s], %s. We will reconnect it soon", self.getUuid(), ret.getError()));
                    reply.setConnected(false);
                } else {
                    boolean connected = self.getUuid().equals(ret.getUuid());
                    if (!connected) {
                        logger.warn(String.format("a signature lost on the virtual router vm[uuid:%s] changed, it's probably caused by the agent restart. We will issue a reconnect soon", self.getUuid()));
                    } else {
                        connected = ApplianceVmStatus.Connected == getSelf().getStatus();
                    }
                    reply.setConnected(connected);
                    reply.setHaStatus(ret.getHaStatus());
                    if ((ret.getHealthy() != null) && (!ret.getHealthy()) && (ret.getHealthDetail() != null)) {
                        fireServiceUnhealthyCanonicalEvent(inerr("virtual router %s unhealthy, detail %s", getSelf().getUuid(), ret.getHealthDetail()));
                    } else {
                        fireServicehealthyCanonicalEvent();
                    }
                    reply.setServiceHealthList(ret.getServiceHealthList());
                }
                completion.success(reply);
            }

            @Override
            public Class<PingRsp> getReturnClass() {
                return PingRsp.class;
            }
        }, TimeUnit.SECONDS, (long)ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class));
    }

    private void handle(final VirtualRouterOverlayInnerMsg msg) {
        NeedReplyMessage originMsg =  msg.getMessage();

        if (originMsg instanceof APIAttachL3NetworkToVmMsg){
            handle(msg, (APIAttachL3NetworkToVmMsg)originMsg);
        } else if (originMsg instanceof APIDetachL3NetworkFromVmMsg){
            handle(msg, (APIDetachL3NetworkFromVmMsg)originMsg);
        } else if (originMsg instanceof DetachNicFromVmMsg){
            handle(msg, (DetachNicFromVmMsg)originMsg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(final VirtualRouterOverlayInnerMsg msg, final APIAttachL3NetworkToVmMsg originMsg) {
        final VmAttachNicReply reply  = new VmAttachNicReply();
        attachNicInQueue(originMsg, originMsg.getL3NetworkUuid(), originMsg.isApplyToBackend(), new ReturnValueCompletion<VmNicInventory>(msg) {
            @Override
            public void success(VmNicInventory returnValue) {
                reply.setInventroy(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final VirtualRouterOverlayInnerMsg msg, final APIDetachL3NetworkFromVmMsg originMsg) {
        MessageReply reply = new MessageReply();
        detachNicInQueue(originMsg, originMsg.getVmNicUuid(), new ReturnValueCompletion<VmInstanceInventory>(msg) {
            @Override
            public void success(VmInstanceInventory returnValue) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final VirtualRouterOverlayInnerMsg msg, final DetachNicFromVmMsg originMsg) {
        MessageReply reply = new MessageReply();
        detachNicInQueueForNoApi(originMsg, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final APIAttachL3NetworkToVmMsg msg) {
        if (vr.getHaStatus().equals(ApplianceVmHaStatus.NoHa.toString())) {
            super.handleApiMessage(msg);
            return;
        }
        VirtualRouterOverlayInnerMsg innerMsg = new VirtualRouterOverlayInnerMsg();
        innerMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        innerMsg.setMessage(msg);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());

        VirtualRouterOverlayMsg omsg = new VirtualRouterOverlayMsg();
        omsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        omsg.setMessage(innerMsg);

        final APIAttachL3NetworkToVmEvent evt = new APIAttachL3NetworkToVmEvent(msg.getId());
        haBackend.virtualRouterOverlayMsgHandle(omsg, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                evt.setInventory(VmInstanceInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIDetachL3NetworkFromVmMsg msg) {

        if (vr.getHaStatus().equals(ApplianceVmHaStatus.NoHa.toString())) {
            super.handleApiMessage(msg);
            return;
        }
        VirtualRouterOverlayInnerMsg innerMsg = new VirtualRouterOverlayInnerMsg();
        innerMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        innerMsg.setMessage(msg);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());

        VirtualRouterOverlayMsg omsg = new VirtualRouterOverlayMsg();
        omsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        omsg.setMessage(innerMsg);

        final APIDetachL3NetworkFromVmEvent evt = new APIDetachL3NetworkFromVmEvent(msg.getId());
        haBackend.virtualRouterOverlayMsgHandle(omsg, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                evt.setInventory(VmInstanceInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final DetachNicFromVmMsg msg) {
        if (vr.getHaStatus().equals(ApplianceVmHaStatus.NoHa.toString()) || msg.isHaPeer() ) {
            super.handleLocalMessage(msg);
            return;
        }

        VirtualRouterOverlayInnerMsg innerMsg = new VirtualRouterOverlayInnerMsg();
        innerMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        innerMsg.setMessage(msg);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());

        VirtualRouterOverlayMsg omsg = new VirtualRouterOverlayMsg();
        omsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        omsg.setMessage(innerMsg);

        final DetachNicFromVmReply reply = new DetachNicFromVmReply();
        haBackend.virtualRouterOverlayMsgHandle(omsg, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final PingVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("ping-virtualrouter-%s", self.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final PingVirtualRouterVmReply reply = new PingVirtualRouterVmReply(self.getUuid());
                if ((VmInstanceState.Running != self.getState() && VmInstanceState.Unknown != self.getState())
                        || ApplianceVmStatus.Connecting == getSelf().getStatus()) {
                    reply.setDoReconnect(false);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                /* retry 3 times */
                List<Integer> steps = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    steps.add(i);
                }
                List<PingVirtualRouterVmReply> replies = new ArrayList<>();
                new While<>(steps).each((s, wcompl) -> {
                    doPing(self.getUuid(), new ReturnValueCompletion<PingVirtualRouterVmReply>(wcompl) {
                        @Override
                        public void success(PingVirtualRouterVmReply returnValue) {
                            replies.clear();
                            replies.add(returnValue);
                            wcompl.allDone();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to ping the virtual router vm[uuid:%s], %s. We will try again", self.getUuid(), reply.getError()));
                            PingVirtualRouterVmReply reply1 = new PingVirtualRouterVmReply(self.getUuid());

                            reply1.setDoReconnect(true);
                            reply1.setConnected(false);
                            reply1.setError(errorCode);
                            replies.add(reply1);
                            /* wait 1 second and try again */
                            new Retry<Boolean>() {
                                @Override
                                @RetryCondition(times = 1, interval = 1)
                                protected Boolean call() {
                                    return false;
                                }
                            }.run();
                            wcompl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (replies.isEmpty()) {
                            /* this happen in UT case */
                            PingVirtualRouterVmReply reply1 = new PingVirtualRouterVmReply(self.getUuid());
                            reply1.setConnected(true);
                            replies.add(reply1);
                        }
                        bus.reply(msg, replies.get(0));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "ping-virtual-router";
            }
        });


    }

    private void provisionConfig(Message msg, final Completion completion) {
        ApplianceVmStatus oldStatus = getSelf().getStatus();

        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        FlowChain chain = getProvisionConfigChain();
        chain.setName(String.format("virtual-router-%s-provision-config", self.getUuid()));
        chain.getData().put(VirtualRouterConstant.Param.VR.toString(), vr);
        chain.getData().put(Params.isReconnect.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.managementNicIp.toString(), vr.getManagementNic().getIp());
        chain.getData().put(Params.applianceVmUuid.toString(), self.getUuid());

        List<ApplianceVmFirewallRuleVO> vos = Q.New(ApplianceVmFirewallRuleVO.class)
                .eq(ApplianceVmFirewallRuleVO_.applianceVmUuid, getSelf().getUuid())
                .list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (oldStatus == ApplianceVmStatus.Connected) {
                    changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                }

                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final ProvisionVirtualRouterConfigMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final ProvisionVirtualRouterConfigReply reply = new ProvisionVirtualRouterConfigReply();

                provisionConfig(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("provision-config-to-virtual-router-%s", self.getUuid());
            }
        });
    }

    private void handle(final APIProvisionVirtualRouterConfigMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIProvisionVirtualRouterConfigEvent evt = new APIProvisionVirtualRouterConfigEvent(msg.getId());

                provisionConfig(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory((ApplianceVmInventory) getSelfInventory());
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
                return String.format("provision-config-to-virtual-router-%s", self.getUuid());
            }
        });
    }

    private boolean skipConnectVirtualRouter() {
        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            AgentVersionVO agentVersionVO = dbf.findByUuid(self.getUuid(), AgentVersionVO.class);
            if (agentVersionVO == null) {
                return true;
            }
        }

        return false;
    }

    private void handle(final ReconnectVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final ReconnectVirtualRouterVmReply reply = new ReconnectVirtualRouterVmReply();

                if (skipConnectVirtualRouter()) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    if (msg.isStatusChange()) {
                        changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                    }
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                reconnect(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reconnect-virtual-router-%s", self.getUuid());
            }
        });
    }

    protected String buildUrl(String mgmtIp, String path) {
        return vrMgr.buildUrl(mgmtIp, path);
    }

    private void handle(final VirtualRouterAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-commands", syncThreadName);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refreshVO();

                final VirtualRouterAsyncHttpCallReply reply = new VirtualRouterAsyncHttpCallReply();
                if (msg.isCheckStatus() && getSelf().getState() != VmInstanceState.Running) {
                    throw new OperationFailureException(operr("the virtual router[name:%s, uuid:%s, current state:%s] is not running," +
                                    "and cannot perform required operation. Please retry your operation later once it is running", self.getName(), self.getUuid(), self.getState()));
                }

                if (msg.isCheckStatus() && getSelf().getStatus() != ApplianceVmStatus.Connected) {
                    throw new OperationFailureException(operr("virtual router[uuid:%s] is in status of %s that cannot make http call to %s",
                            self.getUuid(), getSelf().getStatus(), msg.getPath()));
                }

                if (vr.getManagementNic() == null) {
                    throw new OperationFailureException(operr("virtual router[uuid:%s] has no management nic that cannot make http call to %s",
                            self.getUuid(), msg.getPath()));
                }

                restf.asyncJsonPost(buildUrl(vr.getManagementNic().getIp(), msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(LinkedHashMap ret) {
                        reply.setResponse(ret);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<LinkedHashMap> getReturnClass() {
                        return LinkedHashMap.class;
                    }
                }, TimeUnit.SECONDS, ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class).longValue());
            }

            @Override
            protected int getSyncLevel() {
                return vrMgr.getParallelismDegree(self.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
    
    private void handle(final APIUpdateVirtualRouterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIUpdateVirtualRouterEvent evt = new APIUpdateVirtualRouterEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                updateVirutalRouter(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VirtualRouterVmVO vrVO = dbf.findByUuid(msg.getVmInstanceUuid(), VirtualRouterVmVO.class);
                        evt.setInventory((VirtualRouterVmInventory.valueOf(vrVO)));
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
                return String.format("update-virtual-router-%s", self.getUuid());
            }
        });
    }

    @Transactional
    protected void replaceVirtualRouterDefaultNetwork(String vrUuid, String oldL3Uuid, String newL3Uuud) {
        defaultL3ConfigProxy.detachNetworkService(vrUuid, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK,
                Collections.singletonList(oldL3Uuid));
        defaultL3ConfigProxy.attachNetworkService(vrUuid, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK,
                Collections.singletonList(newL3Uuud));
    }

    private void updateVirutalRouter(APIUpdateVirtualRouterMsg msg, final Completion completion) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(msg.getVmInstanceUuid(), VirtualRouterVmVO.class);
        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("update-virtual-router-%s", msg.getVmInstanceUuid()));
        fchain.then(new Flow() {
            String __name__ = "update-virtual-router-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                replaceVirtualRouterDefaultNetwork(msg.getVmInstanceUuid(), vrVO.getDefaultRouteL3NetworkUuid(),
                        msg.getDefaultRouteL3NetworkUuid());
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                replaceVirtualRouterDefaultNetwork(msg.getVmInstanceUuid(), msg.getDefaultRouteL3NetworkUuid(),
                        vrVO.getDefaultRouteL3NetworkUuid());
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-virtual-router-backend";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                vrMgr.changeVirutalRouterDefaultL3Network(msg.getVmInstanceUuid(), msg.getDefaultRouteL3NetworkUuid(), vrVO.getDefaultRouteL3NetworkUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                VirtualRouterHaTask task = new VirtualRouterHaTask();
                task.setTaskName(VR_CHANGE_DEFAULT_ROUTE_JOB);
                task.setOriginRouterUuid(msg.getVmInstanceUuid());
                ChangeDefaultRouteTaskData d = new ChangeDefaultRouteTaskData();
                d.setNewL3uuid(msg.getDefaultRouteL3NetworkUuid());
                d.setOldL3uuid(vrVO.getDefaultRouteL3NetworkUuid());
                task.setJsonData(JSONObjectUtil.toJsonString(d));
                haBackend.submitVirtualRouterHaTask(task, completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIReconnectVirtualRouterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIReconnectVirtualRouterEvent evt = new APIReconnectVirtualRouterEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                reconnect(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory((ApplianceVmInventory) getSelfInventory());
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
                return String.format("reconnect-virtual-router-%s", self.getUuid());
            }
        });
    }

    private void reconnect(final Completion completion) {
        ApplianceVmStatus oldStatus = getSelf().getStatus();

        FlowChain chain = getReconnectChain();
        chain.setName(String.format("reconnect-virtual-router-%s", self.getUuid()));
        chain.getData().put(VirtualRouterConstant.Param.VR.toString(), vr);
        chain.getData().put(Param.IS_RECONNECT.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.isReconnect.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.managementNicIp.toString(), vr.getManagementNic().getIp());
        chain.getData().put(Params.applianceVmUuid.toString(), self.getUuid());

        List<ApplianceVmFirewallRuleVO> vos = Q.New(ApplianceVmFirewallRuleVO.class)
                .eq(ApplianceVmFirewallRuleVO_.applianceVmUuid, getSelf().getUuid())
                .list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-connecting";

            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connecting);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (oldStatus == ApplianceVmStatus.Connected) {
                    fireDisconnectedCanonicalEvent(errCode);
                }

                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public List<String> getSnatL3NetworkOnRouter(String vrUuid) {
        return Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.defaultRouteL3NetworkUuid)
                .eq(VirtualRouterVmVO_.uuid, vrUuid).listValues();
    }

    @Override
    public void attachNetworkService(String vrUuid, String networkServiceType, String l3NetworkUuid){
    }

    @Override
    public void detachNetworkService(String vrUuid, String networkServiceType, String l3NetworkUuid){
    }

    public class virtualRouterAfterAttachNicFlow extends NoRollbackFlow {
        @Override
        public void run(FlowTrigger trigger, Map data) {
            boolean applyToVirtualRouter = (boolean)data.get(Param.APPLY_TO_VIRTUALROUTER.toString());
            if (!applyToVirtualRouter) {
                trigger.next();
                return;
            }

            VmNicInventory nicInventory = (VmNicInventory) data.get(Param.VR_NIC.toString());
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nicInventory.getL3NetworkUuid()).find();

            VirtualRouterCommands.ConfigureNicCmd cmd = new VirtualRouterCommands.ConfigureNicCmd();
            VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
            info.setDefaultRoute(false);
            info.setMac(nicInventory.getMac());
            info.setNetmask(nicInventory.getNetmask());
            for (UsedIpInventory ip : nicInventory.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv4) {
                    info.setIp(ip.getIp());
                    info.setGateway(ip.getGateway());
                    info.setNetmask(ip.getNetmask());
                } else {
                    info.setIp6(ip.getIp());
                    info.setGateway6(ip.getGateway());
                    NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ip.getIpRangeUuid()).find();
                    info.setPrefixLength(ipr.getPrefixLen());
                    info.setAddressMode(ipr.getAddressMode());
                }
            }

            L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid()).find();
            info.setCategory(l3NetworkVO.getCategory().toString());
            info.setL2type(l2NetworkVO.getType());
            info.setPhysicalInterface(l2NetworkVO.getPhysicalInterface());
            for (L2NetworkGetVniExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkGetVniExtensionPoint.class)) {
                if (ext.getL2NetworkVniType().equals(l2NetworkVO.getType())) {
                    info.setVni(ext.getL2NetworkVni(l2NetworkVO.getUuid(), vr.getHostUuid()));
                }
            }
            info.setMtu(new MtuGetter().getMtu(l3NetworkVO.getUuid()));
            info.setState(nicInventory.getState());
            cmd.setNics(Arrays.asList(info));

            VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
            cmsg.setCommand(cmd);
            cmsg.setPath(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH);
            cmsg.setVmInstanceUuid(vr.getUuid());
            cmsg.setCheckStatus(true);
            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(cmsg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        trigger.fail(reply.getError());
                        return;
                    }

                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    VirtualRouterCommands.ConfigureNicRsp rsp = re.toResponse(VirtualRouterCommands.ConfigureNicRsp.class);
                    if (rsp.isSuccess()) {
                        logger.debug(String.format("successfully add nic[ip:%s, ip6:%s, mac:%s] to virtual router vm[uuid:%s, ip:%s]",
                                info.getIp(), info.getIp6(), info.getMac(), vr.getUuid(), vr.getManagementNic().getIp()));
                        trigger.next();
                    } else {
                        ErrorCode err = operr("unable to add nic[ip:%s, ip6:%s, mac:%s] to virtual router vm[uuid:%s ip:%s], because %s",
                                info.getIp(), info.getIp6(), info.getMac(), vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError());
                        trigger.fail(err);
                    }
                }
            });
        }
    }

    private class virtualRouterApplyServicesAfterAttachNicFlow implements Flow {
        String __name__ = "virtualRouter-apply-services-afterAttachNic";

        private void virtualRouterApplyServicesAfterAttachNic(Iterator<VirtualRouterAfterAttachNicExtensionPoint> it, VmNicInventory nicInv, Completion completion){
            if (!it.hasNext()) {
                completion.success();
                return;
            }

            VirtualRouterAfterAttachNicExtensionPoint ext = it.next();
            logger.debug(String.format("execute afterAttachNic extension %s", ext.getClass().getSimpleName()));
            ext.afterAttachNic(nicInv, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterApplyServicesAfterAttachNic(it, nicInv, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            boolean applyToVirtualRouter = (boolean)data.get(Param.APPLY_TO_VIRTUALROUTER.toString());
            if (!applyToVirtualRouter) {
                trigger.next();
                return;
            }

            if (nicInv.isIpv6OnlyNic()) {
                trigger.next();
                return;
            }
            
            Iterator<VirtualRouterAfterAttachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterAfterAttachNicExtensionPoint.class).iterator();
            virtualRouterApplyServicesAfterAttachNic(it, nicInv,  new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        }

        private void virtualRouterApplyServicesAfterAttachNicRollback(Iterator<VirtualRouterAfterAttachNicExtensionPoint> it, VmNicInventory nicInv, NoErrorCompletion completion){
            if (!it.hasNext()) {
                completion.done();
                return;
            }

            VirtualRouterAfterAttachNicExtensionPoint ext = it.next();
            ext.afterAttachNicRollback(nicInv, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    virtualRouterApplyServicesAfterAttachNicRollback(it, nicInv, completion);
                }
            });
        }

        @Override
        public void rollback(FlowRollback trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterAfterAttachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterAfterAttachNicExtensionPoint.class).iterator();
            virtualRouterApplyServicesAfterAttachNicRollback(it, nicInv, new NoErrorCompletion() {
                @Override
                public void done() {
                    trigger.rollback();
                }
            });
        }
    }

    @Override
    protected void afterAttachNic(VmNicInventory nicInventory, Completion completion) {
        super.afterAttachNic(nicInventory, true, completion);
    }

    @Override
    protected void afterAttachNic(VmNicInventory nicInventory, boolean applyToBackend, Completion completion) {
        VmNicVO vo = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicInventory.getUuid()).find();
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vo.getL3NetworkUuid()).find();

        if (l3NetworkVO.getCategory().equals(L3NetworkCategory.Private)) {
            vo.setMetaData(GUEST_NIC_MASK.toString());

            UsedIpVO usedIpVO = Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, nicInventory.getUsedIpUuid()).find();
            usedIpVO.setMetaData(GUEST_NIC_MASK.toString());
            dbf.updateAndRefresh(usedIpVO);
        } else {
            vo.setMetaData(ADDITIONAL_PUBLIC_NIC_MASK.toString());
        }
        vo = dbf.updateAndRefresh(vo);
        logger.debug(String.format("updated metadata of vmnic[uuid: %s]", vo.getUuid()));

        VirtualRouterVmVO vrVo = dbf.findByUuid(self.getUuid(), VirtualRouterVmVO.class);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(Param.VR_NIC.toString(), VmNicInventory.valueOf(vo));
        data.put(Param.SNAT.toString(), Boolean.FALSE);
        data.put(Param.VR.toString(), VirtualRouterVmInventory.valueOf(vrVo));
        data.put(Param.APPLY_TO_VIRTUALROUTER.toString(), applyToBackend);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("apply-services-after-attach-nic-%s-from-virtualrouter-%s", nicInventory.getUuid(), nicInventory.getVmInstanceUuid()));
        chain.setData(data);
        chain.then(new virtualRouterAfterAttachNicFlow());
        chain.then(new VirtualRouterCreatePublicVipFlow());
        chain.then(new virtualRouterApplyServicesAfterAttachNicFlow());
        chain.then(haBackend.getAttachL3NetworkFlow());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void virtualRouterAfterDetachNic(Iterator<VirtualRouterAfterDetachNicExtensionPoint> exts, VmNicInventory nicInventory, Completion completion) {
        if (!exts.hasNext()) {
            completion.success();
            return;
        }

        VirtualRouterAfterDetachNicExtensionPoint ext = exts.next();
        ext.afterDetachVirtualRouterNic(nicInventory, new Completion(completion) {
            @Override
            public void success() {
                virtualRouterAfterDetachNic(exts, nicInventory, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                virtualRouterAfterDetachNic(exts, nicInventory, completion);
            }
        });
    }

    @Override
    protected void afterDetachNic(VmNicInventory nicInventory, boolean isRollback, Completion completion) {
        if (isRollback) {
            completion.success();
            return;
        }

        List<VirtualRouterAfterDetachNicExtensionPoint> exts = pluginRgty.getExtensionList(VirtualRouterAfterDetachNicExtensionPoint.class);
        virtualRouterAfterDetachNic(exts.iterator(), nicInventory, new Completion(completion) {
            @Override
            public void success() {
                haBackend.detachL3NetworkFromVirtualRouterHaGroup(nicInventory.getVmInstanceUuid(),
                        nicInventory.getL3NetworkUuid(), isRollback, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
    private static class virtualRouterReleaseVipbeforeDetachNic extends NoRollbackFlow {
        @Autowired
        private VipManager vipMgr;
        String __name__ = "virtualRouter-beforeDetachNic";

        private void virtualRouterReleaseVipServices(Iterator<String> it, VipInventory vip, Completion completion) {
            if (!it.hasNext()) {
                completion.success();
                return;
            }
            String service = it.next();
            VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(service);
            ext.releaseServicesOnVip(vip, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterReleaseVipServices(it, vip, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    /* even failed, continue release */
                    virtualRouterReleaseVipServices(it, vip, completion);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nic = (VmNicInventory) data.get(Param.VR_NIC.toString());
            if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
                trigger.next();
                return;
            }

            if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
                trigger.next();
                return;
            }

            VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                    Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

            /*
            * fixme: to be done, the active will not done in standby vpc router because
            * VipPeerL3NetworkRefVO record has been deleted during that of master router
            * this will result the vip delete action will not done in agent before detach nic and
            * not delete some configure such as vip QoS, ifbx.
            */
            List<VipVO> vips = SQL.New("select distinct vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                    "where ref.vipUuid = vip.uuid and ref.l3NetworkUuid in (:routerNetworks) " +
                    "and vip.l3NetworkUuid = :l3Uuid")
                                  .param("l3Uuid", nic.getL3NetworkUuid())
                                  .param("routerNetworks", vr.getAllL3Networks())
                                  .list();

            if (vips.isEmpty()) {
                trigger.next();
                return;
            }
            ErrorCodeList errList = new ErrorCodeList();
            new While<>(vips).all((vip, completion) -> {
                Set<String> services = vip.getServicesTypes();
                if (services == null || services.isEmpty()) {
                    completion.done();
                    return;
                }
                virtualRouterReleaseVipServices(services.iterator(), VipInventory.valueOf(vip), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errList.getCauses().add(errorCode);
                        completion.done();
                    }
                });
            }).run(new WhileDoneCompletion(trigger) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (errList.getCauses().size() > 0) {
                        trigger.fail(errList.getCauses().get(0));
                    } else {
                        trigger.next();
                    }
                }
            });
        }
    }

    public class virtualRouterbeforeDetachNic extends NoRollbackFlow {
        String __name__ = "virtualRouter-beforeDetachNic";
        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInventory = (VmNicInventory) data.get(Param.VR_NIC.toString());
            VirtualRouterCommands.RemoveNicCmd cmd = new VirtualRouterCommands.RemoveNicCmd();
            VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
            info.setIp(nicInventory.getIp());
            info.setDefaultRoute(false);
            info.setGateway(nicInventory.getGateway());
            info.setMac(nicInventory.getMac());
            info.setNetmask(nicInventory.getNetmask());
            cmd.setNics(Arrays.asList(info));

            VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
            cmsg.setCommand(cmd);
            cmsg.setPath(VirtualRouterConstant.VR_REMOVE_NIC_PATH);
            cmsg.setVmInstanceUuid(vr.getUuid());
            cmsg.setCheckStatus(true);
            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(cmsg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("detach nic[%s] from virtual router vm[uuid:%s, ip:%s] failed because %s",
                                info, vr.getUuid(), vr.getManagementNic().getIp(), reply.getError().getDetails()));
                        trigger.next();
                        return;
                    }

                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    VirtualRouterCommands.RemoveNicRsp rsp = re.toResponse(VirtualRouterCommands.RemoveNicRsp.class);
                    if (rsp.isSuccess()) {
                        logger.debug(String.format("successfully detach nic[%s] from virtual router vm[uuid:%s, ip:%s]",info, vr.getUuid(), vr.getManagementNic()
                                .getIp()));
                        trigger.next();
                    } else {
                        logger.warn(String.format("unable to detach nic[%s] from virtual router vm[uuid:%s ip:%s], because %s",
                                info, vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError()));
                        trigger.next();
                    }
                }
            });
        }
    }

    private class virtualRouterReleaseServicesbeforeDetachNicFlow implements Flow {
        String __name__ = "virtualRouter-release-services-before-detach-nic";

        private void virtualRouterReleaseServices(final Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it, VmNicInventory nicInv, Completion completion) {
            if (!it.hasNext()) {
                completion.success();
                return;
            }

            VirtualRouterBeforeDetachNicExtensionPoint ext = it.next();
            logger.debug(String.format("virtual router release service before detach l3 network for %s", ext.getClass().getSimpleName()));
            ext.beforeDetachNic(nicInv, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterReleaseServices(it, nicInv, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    /* even failed, continue the release process */
                    virtualRouterReleaseServices(it, nicInv, completion);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterBeforeDetachNicExtensionPoint.class).iterator();
            virtualRouterReleaseServices(it, nicInv, new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        }

        private void virtualRouterReleaseServicesRollback(final Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it, VmNicInventory nicInv, NoErrorCompletion completion) {
            if (!it.hasNext()) {
                completion.done();
                return;
            }

            VirtualRouterBeforeDetachNicExtensionPoint ext = it.next();
            ext.beforeDetachNicRollback(nicInv, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    virtualRouterReleaseServicesRollback(it, nicInv, completion);
                }
            });
        }

        @Override
        public void rollback(FlowRollback trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterBeforeDetachNicExtensionPoint.class).iterator();
            virtualRouterReleaseServicesRollback(it, nicInv, new NoErrorCompletion(trigger) {
                @Override
                public void done() {
                    trigger.rollback();
                }
            });
        }
    }

    @Override
    protected void beforeDetachNic(VmNicInventory nicInventory, Completion completion) {
        Map data = new HashMap();
        data.put(Param.VR_NIC.toString(), nicInventory);
        data.put(Param.VR.toString(), vr);
        ApplianceVmVO appvm = Q.New(ApplianceVmVO.class)
                .eq(ApplianceVmVO_.uuid, nicInventory.getVmInstanceUuid()).find();
        if (appvm.getStatus().equals(ApplianceVmStatus.Disconnected)) {
            logger.debug(String.format("appliance vm[uuid: %s] current status is [%s], skip before detach nic",
                    appvm.getUuid(), appvm.getStatus()));
            completion.success();
            return;
        }

        if (appvm.getState().equals(VmInstanceState.Stopped)) {
            logger.debug(String.format("appliance vm[uuid: %s] current state is [%s], skip before detach nic",
                    appvm.getUuid(), appvm.getStatus()));
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("release-services-before-detach-nic-%s-from-virtualrouter-%s", nicInventory.getUuid(), nicInventory.getVmInstanceUuid()));
        chain.setData(data);
        chain.insert(new virtualRouterReleaseServicesbeforeDetachNicFlow());
        chain.then(new virtualRouterReleaseVipbeforeDetachNic());
        chain.then(new virtualRouterbeforeDetachNic());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

}
