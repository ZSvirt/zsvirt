package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallCmd;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallRsp;
import org.zstack.compute.vm.VmInstanceBase;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.utils.RangeSet;
import org.zstack.utils.RangeSet.Range;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.isEmpty;

public abstract class ApplianceVmBase extends VmInstanceBase implements ApplianceVm {
    @Autowired
    private RESTFacade restf;

    static {
        allowedOperations.addState(VmInstanceState.Created, StartNewCreatedApplianceVmMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Starting, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Stopping, ApplianceVmRefreshFirewallMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Rebooting, ApplianceVmRefreshFirewallMsg.class.getName());
    }

    @Autowired
    protected ApplianceVmFacade apvmf;

    protected abstract List<Flow> getPostCreateFlows();
    protected abstract List<Flow> getPostStartFlows();
    protected abstract List<Flow> getPostStopFlows();
    protected abstract List<Flow> getPostRebootFlows();
    protected abstract List<Flow> getPostDestroyFlows();
    protected abstract List<Flow> getPostMigrateFlows();

    public ApplianceVmBase(VmInstanceVO vo) {
        super(vo);
    }

    protected ApplianceVmVO getSelf() {
        return (ApplianceVmVO) self;
    }

    protected ApplianceVmInventory getInventory() {
        return ApplianceVmInventory.valueOf(getSelf());
    }

    protected List<Flow> createBootstrapFlows(HypervisorType hvType) {
        Boolean unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        List<Flow> flows = new ArrayList<Flow>();

        flows.add(apvmf.createBootstrapFlow(hvType));
        if (!unitTestOn) {
            flows.add(new ApplianceVmConnectFlow());
            flows.add(new ApplianceVmDeployAgentFlow());
        }
        flows.add(new ApplianceVmSetFirewallFlow());

        return flows;
    }

    protected List<Flow> createAfterConnectNewCreatedVirtualRouterFlows() {
        return new ArrayList<>();
    }

    @Override
    protected void destroyHook(VmInstanceDeletionPolicy deletionPolicy, Message msg, final Completion completion){
        logger.debug(String.format("deleting appliance vm[uuid:%s], always use Direct deletion policy", self.getUuid()));
        VmInstanceDeletionPolicy policy = (deletionPolicy == VmInstanceDeletionPolicy.DBOnly)
                ? deletionPolicy : VmInstanceDeletionPolicy.Direct;
        super.doDestroy(policy, msg, completion);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ApplianceVmRefreshFirewallMsg) {
            handle((ApplianceVmRefreshFirewallMsg) msg);
        } else if (msg instanceof ApplianceVmAsyncHttpCallMsg) {
            handle((ApplianceVmAsyncHttpCallMsg) msg);
        } else if (msg instanceof ApplianceVmChangeStatusMsg) {
            handle((ApplianceVmChangeStatusMsg)msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void handle(final ApplianceVmAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public int getSyncLevel() {
                return 10;
            }

            @Override
            public String getSyncSignature() {
                return String.format("appliancevm-async-httpcall-%s", self.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final ApplianceVmAsyncHttpCallReply reply = new ApplianceVmAsyncHttpCallReply();
                if (msg.isCheckStatus() && getSelf().getStatus() != ApplianceVmStatus.Connected) {
                    reply.setError(operr("appliance vm[uuid:%s] is in status of %s that cannot make http call to %s",
                            self.getUuid(), getSelf().getStatus(), msg.getPath()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                MessageCommandRecorder.record(msg.getCommandClassName());

                restf.asyncJsonPost(buildUrl(msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, chain) {
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
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    protected void handle(final ApplianceVmRefreshFirewallMsg msg) {
        if (msg.isInSyncThread()) {
            thdf.chainSubmit(new ChainTask(msg) {
                @Override
                public String getSyncSignature() {
                    return syncThreadName;
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    refreshFirewall(msg, new NoErrorCompletion(chain) {
                        @Override
                        public void done() {
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return "appliance-vm-refresh-firewall";
                }
            });
        } else {
            refreshFirewall(msg, new NoErrorCompletion(msg) {
                @Override
                public void done() {
                    // nothing
                }
            });
        }
    }

    protected void handle(final ApplianceVmChangeStatusMsg msg) {
        ApplianceVmChangeStatusReply reply = new ApplianceVmChangeStatusReply();
        try {
            changeApplianceVmStatus(msg.getNewStatus());
        } catch (Exception e) {
            reply.setSuccess(false);
        }
        bus.reply(msg, reply);
    }

    public static String buildAgentUrl(String hostname, String subPath, int port) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ApplianceVmGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostname);
        }
        ub.port(port);
        if (!"".equals(ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private String buildUrl(String path) {
        VmNicVO nic = findOneOrNull(self.getVmNics(), arg -> arg.getL3NetworkUuid().equals(getSelf().getManagementNetworkUuid()));
        if (nic == null) {
            throw new OperationFailureException(inerr("cannot find the management nic for appliance vm[uuid:%s]", self.getUuid()));
        }
        return buildAgentUrl(nic.getIp(), path, getSelf().getAgentPort());
    }

    private void refreshFirewall(final ApplianceVmRefreshFirewallMsg msg, final NoErrorCompletion completion) {
        class RuleCombiner {
            long total;
            Map<String, List<ApplianceVmFirewallRuleVO>> rules = new HashMap<String, List<ApplianceVmFirewallRuleVO>>();
            List<ApplianceVmFirewallRuleTO> result = new ArrayList<ApplianceVmFirewallRuleTO>();
            Map<String, String> l3NicMacMap = new HashMap<String, String>();

            {
                for (VmNicVO nic : self.getVmNics()) {
                    l3NicMacMap.put(nic.getL3NetworkUuid(), nic.getMac());
                }

                total = Q.New(ApplianceVmFirewallRuleVO.class)
                        .eq(ApplianceVmFirewallRuleVO_.applianceVmUuid, self.getUuid())
                        .count();
            }

            List<ApplianceVmFirewallRuleTO> merge() {
                if (total == 0) {
                    return new ArrayList<ApplianceVmFirewallRuleTO>();
                }

                prepare();
                normalize();
                return result;
            }

            private void normalize() {
                for (List<ApplianceVmFirewallRuleVO> vos : rules.values()) {
                    if (!vos.isEmpty()) {
                        normalize(vos);
                    }
                }
            }

            private void normalize(List<ApplianceVmFirewallRuleVO> vos) {
                String l3Uuid = null;
                String sip = null;
                String dip = null;
                String allowedCidr = null;
                ApplianceVmFirewallProtocol protocol = null;

                RangeSet rset = new RangeSet();
                for (ApplianceVmFirewallRuleVO vo : vos) {
                    if (l3Uuid == null) {
                        l3Uuid = vo.getL3NetworkUuid();
                    }
                    if (sip == null) {
                        sip = vo.getSourceIp();
                    }
                    if (dip == null) {
                        dip = vo.getDestIp();
                    }
                    if (allowedCidr == null) {
                        allowedCidr = vo.getAllowCidr();
                    }
                    if (protocol == null) {
                        protocol = vo.getProtocol();
                    }
                    rset.closed(vo.getStartPort(), vo.getEndPort());
                }

                List<Range> rs = rset.merge();
                for (Range r : rs) {
                    ApplianceVmFirewallRuleTO to = new ApplianceVmFirewallRuleTO();
                    to.setDestIp(dip);
                    to.setNicMac(l3NicMacMap.get(l3Uuid));
                    to.setProtocol(protocol.toString());
                    to.setAllowCidr(allowedCidr);
                    to.setSourceIp(sip);
                    to.setStartPort((int) r.getStart());
                    to.setEndPort((int) r.getEnd());
                    result.add(to);
                }
            }

            private void prepare() {
                int offset = 0;
                int step = 1000;
                while (offset < total) {
                    List<ApplianceVmFirewallRuleVO> vos = Q.New(ApplianceVmFirewallRuleVO.class)
                            .eq(ApplianceVmFirewallRuleVO_.applianceVmUuid, self.getUuid())
                            .limit(step)
                            .start(offset)
                            .list();
                    for (ApplianceVmFirewallRuleVO vo : vos) {
                        String key = String.format("%s-%s-%s-%s-%s",
                                vo.getL3NetworkUuid(), vo.getProtocol(), vo.getSourceIp(), vo.getDestIp(), vo.getAllowCidr());
                        List<ApplianceVmFirewallRuleVO> lst = rules.get(key);
                        if (lst == null) {
                            lst = new ArrayList<ApplianceVmFirewallRuleVO>();
                            rules.put(key, lst);
                        }
                        lst.add(vo);
                    }
                    offset += step;
                }
            }
        }

        final ApplianceVmRefreshFirewallReply reply = new ApplianceVmRefreshFirewallReply();
        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            reply.setError(allowed);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        RefreshFirewallCmd cmd = new RefreshFirewallCmd();
        List<ApplianceVmFirewallRuleTO> tos = new RuleCombiner().merge();
        cmd.setRules(tos);

        restf.asyncJsonPost(buildUrl(ApplianceVmConstant.REFRESH_FIREWALL_PATH), cmd, new JsonAsyncRESTCallback<RefreshFirewallRsp>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(RefreshFirewallRsp ret) {
                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to refresh firewall rules on appliance vm[uuid:%s, name:%s], %s",
                            self.getUuid(), self.getName(), ret.getError()));
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<RefreshFirewallRsp> getReturnClass() {
                return RefreshFirewallRsp.class;
            }
        });
    }

    @Override
    protected VmInstanceVO refreshVO() {
        self = dbf.findByUuid(self.getUuid(), ApplianceVmVO.class);
        return self;
    }

    @Override
    protected VmInstanceSpec buildSpecFromInventory(VmInstanceInventory inv, VmOperation operation) {
        VmInstanceSpec spec = super.buildSpecFromInventory(inv, operation);
        spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSubType.toString(), getSelf().getApplianceVmType());
        return spec;
    }

    private void prepareLifeCycleInfo(FlowChain chain) {
        ApplianceVmPostLifeCycleInfo info = new ApplianceVmPostLifeCycleInfo();
        ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(getSelf());
        L3NetworkVO defaultRouteL3VO = dbf.findByUuid(ainv.getDefaultRouteL3NetworkUuid(), L3NetworkVO.class);
        info.setDefaultRouteL3Network(L3NetworkInventory.valueOf(defaultRouteL3VO));
        info.setManagementNic(ainv.getManagementNic());
        chain.getData().put(ApplianceVmConstant.Params.applianceVmInfoForPostLifeCycle.toString(), info);
    }

    private void prepareFirewallInfo(FlowChain chain) {
        List<ApplianceVmFirewallRuleVO> vos = Q.New(ApplianceVmFirewallRuleVO.class)
                .eq(ApplianceVmFirewallRuleVO_.applianceVmUuid, getSelf().getUuid())
                .list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);
    }

    @Override
    protected FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getStopVmWorkFlowChain(inv);
        chain.setName(String.format("stop-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s stopped",
                            getSelf().getUuid()));
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subStopFlows = getPostStopFlows();
        if (subStopFlows != null) {
            for (Flow f : subStopFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    protected void fireServiceUnhealthyCanonicalEvent(ErrorCode err) {
        ApplianceVmCanonicalEvents.ServiceHealthData data = new ApplianceVmCanonicalEvents.ServiceHealthData();
        data.setApplianceVmType(getSelf().getApplianceVmType());
        data.setApplianceVmUuid(self.getUuid());
        data.setHealthy(false);
        data.setReason(err);

        evtf.fire(ApplianceVmCanonicalEvents.SERVICE_UNHEALTHY_PATH, data);
    }

    protected void fireServicehealthyCanonicalEvent() {
        ApplianceVmCanonicalEvents.ServiceHealthData data = new ApplianceVmCanonicalEvents.ServiceHealthData();
        data.setApplianceVmType(getSelf().getApplianceVmType());
        data.setApplianceVmUuid(self.getUuid());
        data.setHealthy(true);

        evtf.fire(ApplianceVmCanonicalEvents.SERVICE_HEALTHY_PATH, data);
    }

    protected void fireDisconnectedCanonicalEvent(ErrorCode err) {
        ApplianceVmCanonicalEvents.DisconnectedData data = new ApplianceVmCanonicalEvents.DisconnectedData();
        data.setApplianceVmType(getSelf().getApplianceVmType());
        data.setApplianceVmUuid(self.getUuid());
        data.setReason(err);

        evtf.fire(ApplianceVmCanonicalEvents.DISCONNECTED_PATH, data);
    }

    @Override
    protected FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getDestroyVmWorkFlowChain(inv);
        chain.setName(String.format("destroy-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subDestroyFlows = getPostDestroyFlows();
        if (subDestroyFlows != null) {
            for (Flow f : subDestroyFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        FlowChain chain = super.getMigrateVmWorkFlowChain(inv);
        chain.setName(String.format("migrate-appliancevm-%s", inv.getUuid()));
        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        List<Flow> subMigrateFlows = getPostMigrateFlows();
        if (subMigrateFlows != null) {
            for (Flow f : subMigrateFlows) {
                chain.then(f);
            }
        }

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    protected void provisionAfterRebootVm(VmInstanceSpec spec, NoErrorCompletion completion) {
        if (ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.setName(String.format("provision-appliancevm-after-reboot-%s", spec.getVmInventory().getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connecting);
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s reboot",
                            getSelf().getUuid()));
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s reboot failed",
                            getSelf().getUuid()));
                }
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        chain.then(setApplianceStateRunningFlow());
        addBootstrapFlows(chain, HypervisorType.valueOf(spec.getVmInventory().getHypervisorType()));

        List<Flow> subRebootFlows = getPostRebootFlows();
        if (subRebootFlows != null) {
            for (Flow f : subRebootFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        });
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.debug(String.format("provision applianceVm[uuid:%s, name:%s] failed because %s",
                        spec.getVmInventory().getUuid(),
                        spec.getVmInventory().getName(), errCode.getDetails()));
                completion.done();
            }
        }).start();
    }

    protected void provisionAfterStartVm(VmInstanceSpec spec, NoErrorCompletion completion) {
        if (ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.setName(String.format("provision-appliancevm-after-start-%s", spec.getVmInventory().getUuid()));
        chain.insert(new ApplianceVmSyncConfigToHaGroupFlow());
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
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s start failed",
                            getSelf().getUuid()));
                }
                trigger.rollback();
            }
        });

        chain.then(setApplianceStateRunningFlow());
        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        addBootstrapFlows(chain, HypervisorType.valueOf(spec.getVmInventory().getHypervisorType()));

        List<Flow> subStartFlows = getPostStartFlows();
        if (subStartFlows != null) {
            for (Flow f : subStartFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        });
        chain.then(new ApplianceVmSyncConfigAfterAddToHaGroupFlow());

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.debug(String.format("provision applianceVm[uuid:%s, name:%s] failed because %s",
                        spec.getVmInventory().getUuid(),
                        spec.getVmInventory().getName(), errCode.getDetails()));
                completion.done();
            }
        }).start();
    }

    @Override
    protected FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv) {
        if (!ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
            FlowChain chain = super.getRebootVmWorkFlowChain(inv);
            chain.setName(String.format("reboot-appliancevm-%s", inv.getUuid()));
            boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
            chain.noRollback(noRollbackOnFailure);
            return chain;
        }

        FlowChain chain = super.getRebootVmWorkFlowChain(inv);
        chain.setName(String.format("reboot-appliancevm-%s", inv.getUuid()));
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-disconnected";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connecting);
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s reboot",
                            getSelf().getUuid()));
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s reboot failed",
                            getSelf().getUuid()));
                }
                trigger.rollback();
            }
        });

        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        chain.then(setApplianceStateRunningFlow());
        addBootstrapFlows(chain, HypervisorType.valueOf(inv.getHypervisorType()));

        List<Flow> subRebootFlows = getPostRebootFlows();
        if (subRebootFlows != null) {
            for (Flow f : subRebootFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        });

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    @Override
    protected FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv) {
        if (!ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
            FlowChain chain = super.getStartVmWorkFlowChain(inv);
            chain.setName(String.format("start-appliancevm-%s", inv.getUuid()));
            boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
            chain.noRollback(noRollbackOnFailure);
            return chain;
        }

        FlowChain chain = super.getStartVmWorkFlowChain(inv);
        chain.setName(String.format("start-appliancevm-%s", inv.getUuid()));
        chain.insert(new ApplianceVmSyncConfigToHaGroupFlow());
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
                if (originStatus.equals(ApplianceVmStatus.Connected)) {
                    fireDisconnectedCanonicalEvent(operr("appliance vm %s start failed",
                            getSelf().getUuid()));
                }
                trigger.rollback();
            }
        });

        chain.then(setApplianceStateRunningFlow());
        prepareLifeCycleInfo(chain);
        prepareFirewallInfo(chain);

        addBootstrapFlows(chain, HypervisorType.valueOf(inv.getHypervisorType()));

        List<Flow> subStartFlows = getPostStartFlows();
        if (subStartFlows != null) {
            for (Flow f : subStartFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        });
        chain.then(new ApplianceVmSyncConfigAfterAddToHaGroupFlow());

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        return chain;
    }

    private FlowChain addBootstrapFlows(FlowChain chain, HypervisorType hvType) {
        for (Flow flow : createBootstrapFlows(hvType)) {
            chain.then(flow);
        }

        return chain;
    }

    private FlowChain addAfterConnectNewCreatedVirtualRouterFlows(FlowChain chain) {
        for (Flow flow : createAfterConnectNewCreatedVirtualRouterFlows()) {
            chain.then(flow);
        }

        return chain;
    }

    protected void provisionApplianceVmAfterCreate(VmInstanceSpec spec, ApplianceVmSpec aspec, HypervisorType htype, NoErrorCompletion completion) {
        if (ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();

        chain.setName(String.format("provision-appliancevm-after-create-%s", spec.getVmInventory().getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), aspec.getFirewallRules());

        addBootstrapFlows(chain, htype);

        List<Flow> subCreateFlows = getPostCreateFlows();
        if (subCreateFlows != null) {
            for (Flow f : subCreateFlows) {
                chain.then(f);
            }
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // must reload here, otherwise it will override changes created by previous flows
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        });

        addAfterConnectNewCreatedVirtualRouterFlows(chain);

        boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
        chain.noRollback(noRollbackOnFailure);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.debug(String.format("provision applianceVm[uuid:%s, name:%s] failed because %s",
                        spec.getVmInventory().getUuid(),
                        spec.getVmInventory().getName(), errCode.getDetails()));
                completion.done();
            }
        }).start();
    }


    @Override
    protected void instantiateVmFromNewCreate(final InstantiateNewCreatedVmInstanceMsg msg, final SyncTaskChain taskChain) {
        boolean callNext = true;
        try {
            refreshVO();
            ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
            if (allowed != null) {
                bus.replyErrorByMessageType(msg, allowed);
                return;
            }
            ErrorCode preCreated = extEmitter.preStartNewCreatedVm(msg.getVmInstanceInventory());
            if (preCreated != null) {
                bus.replyErrorByMessageType(msg, preCreated);
                return;
            }

            StartNewCreatedApplianceVmMsg smsg = (StartNewCreatedApplianceVmMsg) msg;
            ApplianceVmSpec aspec = smsg.getApplianceVmSpec();

            final VmInstanceSpec spec = new VmInstanceSpec();
            spec.setVmInventory(msg.getVmInstanceInventory());
            spec.setRequiredPrimaryStorageUuidForRootVolume(smsg.getApplianceVmSpec().getPrimaryStorageUuidForRootVolume());
            spec.setRootVolumeSystemTags(smsg.getApplianceVmSpec().getRootVolumeSystemTags());
            spec.setRequiredHostUuid(smsg.getApplianceVmSpec().getRequiredHostUuid());
            List<VmNicSpec> nicSpecs = new ArrayList<>();
            if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
                List<L3NetworkVO> vos = Q.New(L3NetworkVO.class)
                        .in(L3NetworkVO_.uuid, VmNicSpec.getL3UuidsOfSpec(msg.getL3NetworkUuids()))
                        .list();
                for (L3NetworkVO vo :vos) {
                    nicSpecs.add(new VmNicSpec(L3NetworkInventory.valueOf(vo)));
                }
                spec.setL3Networks(nicSpecs);
            } else {
                spec.setL3Networks(new ArrayList<VmNicSpec>(0));
            }

            if (!isEmpty(msg.getDeprecatedDataVolumeSpecs())) {
                List<String> uuidList = msg.getDeprecatedDataVolumeSpecs().stream()
                        .map(DiskAO::getDiskOfferingUuid)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!uuidList.isEmpty()) {
                    List<Tuple> tuples = Q.New(DiskOfferingVO.class)
                            .in(DiskOfferingVO_.uuid, uuidList)
                            .select(DiskOfferingVO_.uuid, DiskOfferingVO_.diskSize)
                            .listTuple();

                    for (Tuple tuple : tuples) {
                        String uuid = tuple.get(0, String.class);
                        Long diskSize = tuple.get(1, Long.class);
                        for (DiskAO diskAO : msg.getDeprecatedDataVolumeSpecs()) {
                            if (diskAO.getDiskOfferingUuid().equals(uuid)) {
                                diskAO.setSize(diskSize);
                            }
                        }
                    }
                }
            }

            ImageVO imvo = dbf.findByUuid(spec.getVmInventory().getImageUuid(), ImageVO.class);
            spec.getImageSpec().setInventory(ImageInventory.valueOf(imvo));

            spec.putExtensionData(ApplianceVmConstant.Params.timeout.toString(), msg.getTimeout());
            spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), aspec);
            spec.setCurrentVmOperation(VmInstanceConstant.VmOperation.NewCreate);
            spec.putExtensionData(ApplianceVmConstant.Params.applianceVmSubType.toString(), getSelf().getApplianceVmType());
            spec.setBootOrders(list(VmBootDevice.HardDisk.toString()));

            changeVmStateInDb(VmInstanceStateEvent.starting);

            HypervisorType htype = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imvo.getFormat());
            extEmitter.beforeStartNewCreatedVm(VmInstanceInventory.valueOf(self));
            FlowChain chain = apvmf.getCreateApplianceVmWorkFlowBuilder().build();
            setFlowMarshaller(chain);

            chain.setName(String.format("create-appliancevm-%s", msg.getVmInstanceUuid()));
            chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
            chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), aspec.getFirewallRules());

            chain.then(setApplianceStateRunningFlow());
            if (ApplianceVmGlobalConfig.AUTO_ROLLBACK.value(Boolean.class)) {
                addBootstrapFlows(chain, VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imvo.getFormat()));

                List<Flow> subCreateFlows = getPostCreateFlows();
                if (subCreateFlows != null) {
                    for (Flow f : subCreateFlows) {
                        chain.then(f);
                    }
                }

                chain.then(new NoRollbackFlow() {
                    String __name__ = "change-appliancevm-status-to-connected";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // must reload here, otherwise it will override changes created by previous flows
                        changeApplianceVmStatus(ApplianceVmStatus.Connected);
                        trigger.next();
                    }
                });

                addAfterConnectNewCreatedVirtualRouterFlows(chain);
            }

            boolean noRollbackOnFailure = ApplianceVmGlobalProperty.NO_ROLLBACK_ON_POST_FAILURE;
            chain.noRollback(noRollbackOnFailure);
            chain.done(new FlowDoneHandler(msg, taskChain) {
                @Override
                public void handle(Map data) {
                    provisionApplianceVmAfterCreate(spec, aspec, htype, new NoErrorCompletion(taskChain) {
                        @Override
                        public void done() {
                            VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                            changeVmStateInDb(VmInstanceStateEvent.running, () -> new SQLBatch() {
                                @Override
                                protected void scripts() {
                                    self.setLastHostUuid(spec.getDestHost().getUuid());
                                    self.setHostUuid(spec.getDestHost().getUuid());
                                    self.setClusterUuid(spec.getDestHost().getClusterUuid());
                                    self.setZoneUuid(spec.getDestHost().getZoneUuid());
                                    self.setHypervisorType(spec.getDestHost().getHypervisorType());
                                    self.setRootVolumeUuid(spec.getDestRootVolume().getUuid());
                                }
                            }.execute());

                            logger.debug(String.format("appliance vm[uuid:%s, name: %s, type:%s] is running ..",
                                    self.getUuid(), self.getName(), getSelf().getApplianceVmType()));
                            VmInstanceInventory inv = VmInstanceInventory.valueOf(self);
                            extEmitter.afterStartNewCreatedVm(inv);
                            InstantiateNewCreatedVmInstanceReply reply = new InstantiateNewCreatedVmInstanceReply();
                            reply.setVmInventory(inv);
                            bus.reply(msg, reply);
                            taskChain.next();
                        }
                    });
                }
            }).error(new FlowErrorHandler(msg, taskChain) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    extEmitter.failedToStartNewCreatedVm(VmInstanceInventory.valueOf(self), errCode);
                    dbf.remove(self);
                    InstantiateNewCreatedVmInstanceReply reply = new InstantiateNewCreatedVmInstanceReply();
                    reply.setError(errCode);
                    reply.setSuccess(false);
                    bus.reply(msg, reply);
                    taskChain.next();
                }
            }).start();

            callNext = false;
        } finally {
            if (callNext) {
                taskChain.next();
            }
        }
    }

    @Override
    protected void selectDefaultL3(VmNicInventory nic) {
        return;
    }

    protected void changeApplianceVmStatus(ApplianceVmStatus newStatus) {
        self = dbf.reload(self);
        if (newStatus == getSelf().getStatus()) {
            return;
        }
        ApplianceVmStatus oldStatus = getSelf().getStatus();
        logger.debug(String.format("ApplianceVm [%s] changed status, old status: [%s], new status: [%s]", self.getUuid(), getInventory().getStatus(), newStatus.toString()));
        getSelf().setStatus(newStatus);
        self = dbf.updateAndRefresh(self);
        ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData d = new ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData();
        d.setApplianceVmUuid(self.getUuid());
        d.setApplianceVmType(getSelf().getApplianceVmType());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(newStatus.toString());
        d.setInv(ApplianceVmInventory.valueOf(getSelf()));
        evtf.fire(ApplianceVmCanonicalEvents.APPLIANCEVM_STATUS_CHANGED_PATH, d);

        logger.debug(String.format("the virtual router [uuid:%s, name:%s] changed status from %s to %s",
                self.getUuid(), self.getName(), oldStatus.toString(), newStatus.toString()));
    }
    
    private Flow setApplianceStateRunningFlow() {
        return new NoRollbackFlow() {
            String __name__ = "set applianceVm state running";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeVmStateInDb(VmInstanceStateEvent.running);
                trigger.next();
            }
        };
    }
}
