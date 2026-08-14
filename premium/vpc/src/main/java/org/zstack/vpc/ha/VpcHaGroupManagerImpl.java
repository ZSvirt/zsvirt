package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaCallbackStruct;
import org.zstack.header.network.service.VirtualRouterHaGetCallbackExtensionPoint;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.vm.*;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.*;
import org.zstack.identity.Account;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStopVmExtensionPoint;
import org.zstack.kvm.KvmVmSyncExtensionPoint;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.vip.VipState;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.ha.vyos.VpcHaRouterCommands;
import org.zstack.vpc.ha.vyos.vyosVpcHaRouterBackendManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;

public class VpcHaGroupManagerImpl extends AbstractService implements VpcHaGroupManager, VmInstanceStopExtensionPoint, VmInstanceDestroyExtensionPoint, PrepareDbInitialValueExtensionPoint,
        KVMStopVmExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint, VmInstanceStartExtensionPoint, VmInstanceRebootExtensionPoint, KvmVmSyncExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VpcHaGroupManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    vyosVpcHaRouterBackendManager vyosHaBackend;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private RESTFacade restf;

    private final String CHANGE_MONITOR_IP_TASK = "ChangeMonitorIp";

    private String getThreadSyncSignature(String uuid) {
        return String.format("vpcha-group-%s", uuid);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateVpcHaGroupMsg) {
            handle((APICreateVpcHaGroupMsg) msg);
        } else if (msg instanceof APIDeleteVpcHaGroupMsg) {
            handle((APIDeleteVpcHaGroupMsg) msg);
        } else if (msg instanceof VpcHaGroupDeletionMsg) {
            handle((VpcHaGroupDeletionMsg) msg);
        } else if (msg instanceof APIUpdateVpcHaGroupMsg) {
            handle((APIUpdateVpcHaGroupMsg) msg);
        } else if (msg instanceof APIChangeVpcHaGroupMonitorIpsMsg) {
            handle((APIChangeVpcHaGroupMonitorIpsMsg) msg);
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        }  else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(OverlayMessage msg) {
        if (msg instanceof VirtualRouterOverlayMsg) {
            handle((VirtualRouterOverlayMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(VirtualRouterOverlayMsg msg) {

        String haUuid = haBackend.getVirtualRouterHaUuid(msg.getVmInstanceUuid());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(haUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doOverlayMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                VirtualRouterOverlayInnerMsg innerMsg = (VirtualRouterOverlayInnerMsg) msg.getMessage();

                return String.format("ha-%s-%s", innerMsg.getMessageName(), haUuid);
            }
        });
    }

    private void doOverlayMessage(VirtualRouterOverlayMsg msg, NoErrorCompletion noErrorCompletion) {
        VmInstanceVO vmvo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmvo == null) {
            bus.reply(msg, new MessageReply());
            noErrorCompletion.done();
        }

        bus.send(msg.getMessage(), new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply reply) {
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void submitChangeMonitorIpToHaRouter(String vrUuid, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(CHANGE_MONITOR_IP_TASK);
        task.setOriginRouterUuid(vrUuid);
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private void handle(APIChangeVpcHaGroupMonitorIpsMsg msg) {
        APIChangeVpcHaGroupMonitorIpsEvent event = new APIChangeVpcHaGroupMonitorIpsEvent(msg.getId());
        VpcHaGroupVO vo = dbf.findByUuid(msg.getUuid(), VpcHaGroupVO.class);

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(vo.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("add-vpcha-group-monitor-ip-%s", vo.getUuid()));
                fchain.then(new Flow() {
                    String __name__ = "add-vpcha-group-monitor-ip-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VpcHaGroupMonitorIpVO> oldMonitors = Q.New(VpcHaGroupMonitorIpVO.class)
                                .eq(VpcHaGroupMonitorIpVO_.vpcHaRouterUuid, msg.getUuid()).list();
                        List<VpcHaGroupMonitorIpVO> newMonitors = new ArrayList<>();
                        for (String ip : msg.getMonitorIps()) {
                            VpcHaGroupMonitorIpVO monitorIpVO = new VpcHaGroupMonitorIpVO();
                            monitorIpVO.setVpcHaRouterUuid(msg.getUuid());
                            monitorIpVO.setMonitorIp(ip);
                            newMonitors.add(monitorIpVO);
                        }

                        dbf.removeCollection(oldMonitors, VpcHaGroupMonitorIpVO.class);
                        dbf.persistCollection(newMonitors);

                        data.put(APIChangeVpcHaGroupMonitorIpsMsg.class.getSimpleName(), oldMonitors);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<VpcHaGroupMonitorIpVO> oldMonitors = (List<VpcHaGroupMonitorIpVO>)
                                data.get(APIChangeVpcHaGroupMonitorIpsMsg.class.getSimpleName());

                        SQL.New(VpcHaGroupMonitorIpVO.class).eq(VpcHaGroupMonitorIpVO_.vpcHaRouterUuid, msg.getUuid()).delete();
                        dbf.persistCollection(oldMonitors);
                        trigger.rollback();
                    }
                }).then(new Flow() {
                    String __name__ = "add-vpcha-group-monitor-ip-to-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String masterUuid = VpcHaGroupOperator.getMasterUuidByVpcHaRouterUuid(msg.getUuid());
                        if (masterUuid == null){
                            trigger.next();
                            return;
                        }

                        vyosHaBackend.enableHa(masterUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                submitChangeMonitorIpToHaRouter(masterUuid, new Completion(trigger) {
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

                            @Override
                            public void fail(ErrorCode errorCode) {

                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                }).done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        VpcHaGroupVO finalVo = dbf.reload(vo);
                        event.setInventory(VpcHaGroupInventory.valueOf(finalVo));
                        bus.publish(event);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        event.setError(errCode);
                        bus.publish(event);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return "add-vpcha-group-monitor-ip";
            }
        });
    }

    private void handle(APIUpdateVpcHaGroupMsg msg) {
        APIUpdateVpcHaGroupEvent event = new APIUpdateVpcHaGroupEvent(msg.getId());
        VpcHaGroupVO vo = dbf.findByUuid(msg.getUuid(), VpcHaGroupVO.class);
        boolean changed = false;

        if (msg.getName() != null && !msg.getName().equals(vo.getName())) {
            vo.setName(msg.getName());
            changed = true;
        }

        if (msg.getDescription() != null && !msg.getDescription().equals(vo.getDescription())) {
            vo.setDescription(msg.getDescription());
            changed = true;
        }

        if (changed) {
            vo = dbf.updateAndRefresh(vo);
        }

        event.setInventory(VpcHaGroupInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateVpcHaGroupMsg msg) {
        final APICreateVpcHaGroupEvent event = new APICreateVpcHaGroupEvent(msg.getId());
        createVpcHaGroup(msg, new ReturnValueCompletion<VpcHaGroupInventory>(msg) {
            @Override
            public void success(VpcHaGroupInventory returnValue) {
                event.setInventory(returnValue);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                event.setSuccess(false);
                bus.publish(event);
            }
        });
    }

    private void doDeleteVpcHaRouter(APIDeleteVpcHaGroupMsg msg, Completion completion){
        final String issuer = VpcHaGroupVO.class.getSimpleName();
        VpcHaGroupVO vo = dbf.findByUuid(msg.getUuid(), VpcHaGroupVO.class);
        final List<VpcHaGroupInventory> ctx = asList(VpcHaGroupInventory.valueOf(vo));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vpcHa-router-%s", msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vpcHa-router-permissive-check";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vpcHa-router-permissive-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vpcHa-router-force-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APIDeleteVpcHaGroupMsg msg) {
        APIDeleteVpcHaGroupEvent evt = new APIDeleteVpcHaGroupEvent(msg.getId());
        doDeleteVpcHaRouter(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(VpcHaGroupDeletionMsg msg) {
        VpcHaGroupDeletionReply reply = new VpcHaGroupDeletionReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeletionVpcHaRouter(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(msg.getUuid(), VpcHaGroupVO.class);
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
                return String.format("VpcHaRouterDeletion-%s", msg.getUuid());
            }
        });

    }

    private void doDeletionVpcHaRouter(VpcHaGroupDeletionMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-vpcHa-router-%s", msg.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-vpcha-virtual-router";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vpcUuids = Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, msg.getUuid())
                        .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();

                if (vpcUuids == null || vpcUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<VirtualRouterVmVO> vrVos = Q.New(VirtualRouterVmVO.class).in(VirtualRouterVmVO_.uuid, vpcUuids).list();
                if (vrVos == null || vrVos.isEmpty()) {
                    trigger.next();
                    return;
                }
                List<VirtualRouterVmInventory> vrInv = VirtualRouterVmInventory.valueOf2(vrVos);
                deleteVirtualRouter(vrInv, new NoErrorCompletion(trigger){
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-affinityGroup-for-vpcha";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String affinityGroupUuid = VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP.getTokenByResourceUuid(msg.getUuid(), VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP_TOKEN);
                AffinityGroupDeletionMsg affinityGroupDeletionMsg = new AffinityGroupDeletionMsg();
                affinityGroupDeletionMsg.setTimeout(TimeUnit.SECONDS.toMillis(ApplianceVmGlobalConfig.DELETE_TIMEOUT.value(Long.class)));
                affinityGroupDeletionMsg.setUuid(affinityGroupUuid);
                bus.makeTargetServiceIdByResourceUuid(affinityGroupDeletionMsg, AffinityGroupConstants.SERVICE_ID, affinityGroupUuid);
                bus.send(affinityGroupDeletionMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if(!reply.isSuccess()){
                            logger.debug(String.format("delete affinity group of vpc ha group failed [uuid:%s]", affinityGroupUuid));
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(msg){
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void deleteVirtualRouter(List<VirtualRouterVmInventory> vrInvs, final NoErrorCompletion completion) {
        if (vrInvs.isEmpty()) {
            completion.done();
            return;
        }

        new While<>(vrInvs).step((vrInv, compl) -> {
            VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
            msg.setIgnoreResourceReleaseFailure(true);
            msg.setForceDelete(true);
            msg.setVmInstanceUuid(vrInv.getUuid());
            msg.setTimeout(TimeUnit.SECONDS.toMillis(ApplianceVmGlobalConfig.DELETE_TIMEOUT.value(Long.class)));
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrInv.getUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
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


    private void createVpcHaGroup(final APICreateVpcHaGroupMsg msg, final ReturnValueCompletion<VpcHaGroupInventory> completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-vpcHa-router-%s", msg.getName()));
        chain.then(new Flow() {
            String __name__ = "persist-vpcHa-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                /* create vpcHa Group */
                VpcHaGroupVO ha = new VpcHaGroupVO();
                ha.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
                ha.setAccountUuid(msg.getSession().getAccountUuid());
                ha.setDescription(msg.getDescription());
                ha.setName(msg.getName());
                ha = dbf.persistAndRefresh(ha);

                chain.getData().put(VpcHaGroupConstants.Params.VPCHA_INVENTORY.toString(), ha);

                List<VpcHaGroupMonitorIpVO> monitorIpVOS = new ArrayList<>();
                for (String ip : msg.getMonitorIps()) {
                    VpcHaGroupMonitorIpVO monitorIpVO = new VpcHaGroupMonitorIpVO();
                    monitorIpVO.setMonitorIp(ip);
                    monitorIpVO.setVpcHaRouterUuid(ha.getUuid());
                    monitorIpVOS.add(monitorIpVO);
                }
                if (!monitorIpVOS.isEmpty()) {
                    dbf.persistCollection(monitorIpVOS);
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VpcHaGroupVO ha = (VpcHaGroupVO) chain.getData().get(VpcHaGroupConstants.Params.VPCHA_INVENTORY.toString());
                dbf.removeByPrimaryKey(ha.getUuid(), VpcHaGroupVO.class);
                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__ = "create-affinityGroup-for-vpcha";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VpcHaGroupVO ha = (VpcHaGroupVO) chain.getData().get(VpcHaGroupConstants.Params.VPCHA_INVENTORY.toString());
                CreateAffinityGroupMsg cmsg = new CreateAffinityGroupMsg();
                cmsg.setName(VpcHaGroupOperator.getVpcHaRouterAffinityGroupName(ha.getName()));
                cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                cmsg.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
                cmsg.setType(AffinityGroupType.HOST.toString());
                cmsg.setApplianceType(AffinityGroupAppliance.VROUTER_HA.toString());
                bus.makeLocalServiceId(cmsg, AffinityGroupConstants.SERVICE_ID);
                MessageReply reply = bus.call(cmsg);
                if (!reply.isSuccess()) {
                    trigger.fail(operr("create affinityGroup for ha group [uuid:%s] failed", ha.getName()));
                    return;
                }

                CreateAffinityGroupReply areply = (CreateAffinityGroupReply) reply;
                chain.getData().put(VpcHaGroupConstants.Params.AFFINITYGROUP_INVENTORY.toString(), areply.getAffinityGroup());

                SystemTagCreator creator = VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP.newSystemTagCreator(ha.getUuid());
                creator.setTagByTokens(CollectionDSL.map(
                        e(VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP_TOKEN, areply.getAffinityGroup().getUuid())
                ));
                creator.create();

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                AffinityGroupInventory agInv = (AffinityGroupInventory)chain.getData().get(VpcHaGroupConstants.Params.AFFINITYGROUP_INVENTORY.toString());
                AffinityGroupDeletionMsg dmsg = new AffinityGroupDeletionMsg();
                dmsg.setUuid(agInv.getUuid());
                dmsg.setForceDelete(true);
                bus.makeTargetServiceIdByResourceUuid(dmsg, AffinityGroupConstants.SERVICE_ID, agInv.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.debug(String.format("delete AffinityGroup[uuid:%s] failed", agInv.getUuid()));
                        }
                        trigger.rollback();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                VpcHaGroupVO haVo = (VpcHaGroupVO) chain.getData().get(VpcHaGroupConstants.Params.VPCHA_INVENTORY.toString());
                VpcHaGroupVO vo = dbf.findByUuid(haVo.getUuid(), VpcHaGroupVO.class);
                completion.success(VpcHaGroupInventory.valueOf(vo));
            }
        }).error(new FlowErrorHandler(msg){
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VpcHaGroupConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(VpcHaGroupConstants.VPC_ROUTER_STATUS_REPORT, VpcHaRouterCommands.VpcRouterHaStatusCmd.class, new SyncHttpCallHandler<VpcHaRouterCommands.VpcRouterHaStatusCmd>() {
            @Override
            public String handleSyncHttpCall(VpcHaRouterCommands.VpcRouterHaStatusCmd cmd) {
                try {
                    final ApplianceVmHaStatus status = ApplianceVmHaStatus.valueOf(cmd.haStatus);
                    new SQLBatch() {
                        // returns true if any updates to entities
                        private boolean setVrHaStatus(String vrUuid, ApplianceVmHaStatus status) {
                            ApplianceVmHaStatus flipped = status == ApplianceVmHaStatus.Backup ? ApplianceVmHaStatus.Master : ApplianceVmHaStatus.Backup;
                            int n = sql(ApplianceVmVO.class)
                                    .eq(ApplianceVmVO_.uuid, vrUuid)
                                    .condAnd(ApplianceVmVO_.haStatus, SimpleQuery.Op.EQ, flipped)
                                    .set(ApplianceVmVO_.haStatus, status)
                                    .update();
                            logger.warn(String.format("prepare change vpc [uuid:%s] haStatus changed %s to %s ", vrUuid, flipped.toString(), status.toString()));
                            if (n > 0) {
                                logger.warn(String.format("virtualrouter [uuid:%s] haStatus changed to %s", vrUuid, status.toString()));
                                // when vpc router status changed from master to backup ,alarm fire

                                ApplianceVmHaStatus old = status == ApplianceVmHaStatus.Backup ? ApplianceVmHaStatus.Master : ApplianceVmHaStatus.Backup;
                                fireApplianceVmHaStatusChangedEvent(vrUuid, status, old);

                                return true;
                            }

                            return false;
                        }

                        private void checkAndHandleSplitBrain(String vrUuid, ApplianceVmHaStatus status) {
                            if (status != ApplianceVmHaStatus.Master) {
                                return;
                            }

                            String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
                            if (peerUuid == null) {
                                return;
                            }

                            VpcRouterVmVO peerVo = dbf.findByUuid(peerUuid, VpcRouterVmVO.class);
                            if (peerVo == null || status != peerVo.getHaStatus()) {
                                return;
                            }

                            // In case backup changed to master but original master has not updated yet
                            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                            msg.setPath(VpcHaRouterCommands.SYNC_VPC_ROUTER_HA_PATH);
                            msg.setVmInstanceUuid(peerUuid);
                            msg.setCommand(new VpcHaRouterCommands.SyncVpcRouterHaCmd());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, peerUuid);
                            bus.send(msg, new CloudBusCallBack(null) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to sync haStatus of virtual router vm[uuid:%s], %s", peerUuid, reply.getError()));
                                        return;
                                    }

                                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                                    VpcHaRouterCommands.SyncVpcRouterHaRsp rsp = re.toResponse(VpcHaRouterCommands.SyncVpcRouterHaRsp.class);
                                    if (!rsp.isSuccess()){
                                        logger.warn(String.format("failed to sync haStatus of virtual router vm[uuid:%s], %s", peerUuid, rsp.getError()));
                                        return;
                                    }

                                    ApplianceVmHaStatus peerStatus = ApplianceVmHaStatus.valueOf(rsp.getHaStatus());
                                    if (peerStatus == ApplianceVmHaStatus.Master) {
                                        restartKeepalived(vrUuid);
                                    } else if (peerStatus == ApplianceVmHaStatus.Backup) {
                                        setVrHaStatus(peerUuid, ApplianceVmHaStatus.valueOf(rsp.getHaStatus()));
                                    }

                                }
                            });
                        }

                        private void restartKeepalived(String vrUuid) {
                            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                            msg.setPath(VpcHaRouterCommands.RESTART_KEEPALIVED_PATH);
                            msg.setVmInstanceUuid(vrUuid);
                            msg.setCommand(new VpcHaRouterCommands.RestartKeepalivedCmd());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
                            bus.send(msg, new CloudBusCallBack(null) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to restart keepalived of virtual router vm[uuid:%s], %s", vrUuid, reply.getError()));
                                        return;
                                    }

                                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                                    VpcHaRouterCommands.RestartKeepalivedRsp rsp = re.toResponse(VpcHaRouterCommands.RestartKeepalivedRsp.class);
                                    if (!rsp.isSuccess()){
                                        logger.warn(String.format("failed to restart keepalived of virtual router vm[uuid:%s], %s", vrUuid, rsp.getError()));
                                    } else {
                                        logger.debug(String.format("successfully restarted keepalived of virtual router vm[uuid:%s]", vrUuid));
                                    }
                                }
                            });
                        }

                        @Override
                        protected void scripts() {
                            if (status != ApplianceVmHaStatus.Backup && status != ApplianceVmHaStatus.Master) {
                                return;
                            }

                            if (!setVrHaStatus(cmd.virtualRouterUuid, status)) {
                                return;
                            }

                            checkAndHandleSplitBrain(cmd.virtualRouterUuid, status);

                            flush();
                        }
                    }.execute();
                } catch (Exception e) {
                    logger.debug(String.format("get unknown haStats: %s from virtual router [uuid:%s]",
                            cmd.haStatus, cmd.virtualRouterUuid));
                }

                // TODO Schedule thread to sync new master rules with Reconnect and cancel GC
                return null;
            }
        });

        evtf.onLocal(ApplianceVmCanonicalEvents.APPLIANCEVM_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData d = (ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData) data;
                if (d.getNewStatus().equals(ApplianceVmStatus.Disconnected.toString())) {
                    String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(d.getApplianceVmUuid());
                    if (peerUuid == null) {
                        return;
                    }

                    VpcRouterVmVO peerVo = dbf.findByUuid(peerUuid, VpcRouterVmVO.class);
                    if (peerVo == null) {
                        return;
                    }

                    if (peerVo.getStatus() == ApplianceVmStatus.Connected && peerVo.getHaStatus() == ApplianceVmHaStatus.Master
                            && peerVo.getState() == VmInstanceState.Running) {
                        setVirtualRouterHaStatus(d.getApplianceVmUuid(), ApplianceVmHaStatus.Backup);
                    }
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {
    }

    public boolean setVirtualRouterHaStatus(String vrUuid, ApplianceVmHaStatus status){
        ApplianceVmHaStatus old = status == ApplianceVmHaStatus.Backup ? ApplianceVmHaStatus.Master : ApplianceVmHaStatus.Backup;
        int n = SQL.New(ApplianceVmVO.class)
                .eq(ApplianceVmVO_.uuid, vrUuid)
                .condAnd(ApplianceVmVO_.haStatus, SimpleQuery.Op.EQ, old)
                .set(ApplianceVmVO_.haStatus, status)
                .update();
        if (n > 0) {
            logger.warn(String.format("virtualrouter [uuid:%s] haStatus changed to %s", vrUuid, status.toString()));

            // when vpc router status changed from master to backup ,alarm fire
            fireApplianceVmHaStatusChangedEvent(vrUuid, status, old);

            return true;
        }

        return false;
    }

    public void fireApplianceVmHaStatusChangedEvent(String vrUuid, ApplianceVmHaStatus status,ApplianceVmHaStatus old){
        if (status.equals(ApplianceVmHaStatus.Backup)) {
            String vpcHaGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
            String vpcHaGroupName = Q.New(VpcHaGroupVO.class).select(VpcHaGroupVO_.name).eq(VpcHaGroupVO_.uuid, vpcHaGroupUuid).findValue();
            String vrName = Q.New(VmInstanceVO.class).select(VmInstanceVO_.name).eq(VmInstanceVO_.uuid, vrUuid).findValue();

            ApplianceVmCanonicalEvents.ApplianceVmHaStatusChangedData data = new ApplianceVmCanonicalEvents.ApplianceVmHaStatusChangedData();
            data.setApplianceVmUuid(vrUuid);
            data.setApplianceVmType(VpcConstants.VPC_VROUTER_VM_TYPE);
            data.setReason(operr("virtualrouter %s [uuid: %s ] of VPC HA group %s [uuid: %s] haStatus changed from %s to %s",
                    vrName, vrUuid, vpcHaGroupName, vpcHaGroupUuid, old, status));
            evtf.fire(ApplianceVmCanonicalEvents.APPLIANCEVM_HASTATUS_CHANGED_PATH, data);
        }
    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {
        setVirtualRouterHaStatus(inv.getUuid(), ApplianceVmHaStatus.Backup);
    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {}

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {}

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        setVirtualRouterHaStatus(inv.getUuid(), ApplianceVmHaStatus.Backup);
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {}

    @Override
    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.StopVmCmd cmd) {}

    @Override
    public void stopVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        setVirtualRouterHaStatus(vm.getUuid(), ApplianceVmHaStatus.Backup);
    }

    @Override
    public String preRebootVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeRebootVm(VmInstanceInventory inv) {
        setVirtualRouterHaStatus(inv.getUuid(), ApplianceVmHaStatus.Backup);
    }

    @Override
    public void afterRebootVm(VmInstanceInventory inv) {}

    @Override
    public void failedToRebootVm(VmInstanceInventory inv, ErrorCode reason) {}

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
        setVirtualRouterHaStatus(inv.getUuid(), ApplianceVmHaStatus.Backup);
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {}

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {}

    @Override
    public void stopVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    @Transactional
    public void prepareDbInitialValue() {
        /* create vips for additional public network, and create default network for ha group */
        List<VpcHaGroupVO> haGroups = Q.New(VpcHaGroupVO.class).list();
        List<VipVO> vips = new ArrayList<>();
        List<VpcHaGroupNetworkServiceRefVO> refs = new ArrayList<>();
        for (VpcHaGroupVO ha : haGroups) {
            List<String> vpcUuids = Q.New(VpcHaGroupApplianceVmRefVO.class)
                    .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, ha.getUuid())
                    .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();
            if (vpcUuids.size() == 0) {
                continue;
            }

            VpcRouterVmVO vpc = dbf.findByUuid(vpcUuids.get(0), VpcRouterVmVO.class);
            for (VmNicVO nic : vpc.getVmNics()) {
                if (VirtualRouterNicMetaData.isManagementNic(nic)) {
                    continue;
                }

                if (VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
                    if (Q.New(VipVO.class).eq(VipVO_.ip, nic.getIp()).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).isExists()) {
                        continue;
                    }

                    L3NetworkInventory vipL3 = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
                    IpRangeInventory ipRange = null;
                    for (IpRangeInventory ipr : IpRangeHelper.getNormalIpRanges(vipL3, IPv6Constants.IPv4)) {
                        if (NetworkUtils.isIpv4InRange(nic.getIp(), ipr.getStartIp(), ipr.getEndIp())) {
                            ipRange = ipr;
                            break;
                        }
                    }

                    if (ipRange == null){
                        logger.debug(String.format("can not find ip range ip address[ip:%s, l3 network: %s]",
                                nic.getIp(), nic.getL3NetworkUuid()));
                        continue;
                    }

                    VipVO vipvo = new VipVO();
                    vipvo.setUuid(Platform.getUuid());
                    vipvo.setName(String.format("vip-%s", ha.getName()));
                    vipvo.setDescription(String.format("system vip for %s", ha.getName()));
                    vipvo.setState(VipState.Enabled);
                    vipvo.setGateway(nic.getGateway());
                    vipvo.setIp(nic.getIp());
                    vipvo.setIpRangeUuid(ipRange.getUuid());
                    vipvo.setL3NetworkUuid(nic.getL3NetworkUuid());
                    vipvo.setNetmask(ipRange.getNetmask());
                    vipvo.setUsedIpUuid(nic.getUsedIpUuid());
                    vipvo.setAccountUuid(Account.getAccountUuidOfResource(ha.getUuid()));
                    vipvo.setSystem(true);
                    vipvo.setPrefixLen(ipRange.getPrefixLen());
                    vips.add(vipvo);

                    VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
                    ref.setVpcHaRouterUuid(ha.getUuid());
                    ref.setNetworkServiceName(VipVO.class.getSimpleName());
                    ref.setNetworkServiceUuid(vipvo.getUuid());
                    refs.add(ref);
                }
            }

            VpcHaGroupNetworkServiceRefVO ref = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, ha.getUuid())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK).limit(1)
                    .find();
            if (ref == null) {
                ref = new VpcHaGroupNetworkServiceRefVO();
                ref.setVpcHaRouterUuid(ha.getUuid());
                ref.setNetworkServiceName(VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK);
                ref.setNetworkServiceUuid(vpc.getDefaultRouteL3NetworkUuid());
                refs.add(ref);
            }
        }

        if (!vips.isEmpty()) {
            dbf.persistCollection(vips);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct syncRoute = new VirtualRouterHaCallbackStruct();
        syncRoute.type = CHANGE_MONITOR_IP_TASK;
        syncRoute.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                vyosHaBackend.enableHa(vrUuid, completion);
            }
        };
        structs.add(syncRoute);

        return structs;
    }

    @Override
    public void afterVmSync(HostInventory host, Map<String, VmInstanceState> states, Set<String> vmsToSkipSetHostSide) {
        // if vpc is paused from host side, do not change it to backup when it is master
        states.forEach((vrUuid, newState) -> {
            if (newState == VmInstanceState.Stopped) {
                boolean hasChangedHaStatus = setVirtualRouterHaStatus(vrUuid, ApplianceVmHaStatus.Backup);
                if (hasChangedHaStatus) {
                    ApplianceVmChangeStatusMsg msg = new ApplianceVmChangeStatusMsg();
                    msg.setNewStatus(ApplianceVmStatus.Disconnected);
                    msg.setVmInstanceUuid(vrUuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                    bus.send(msg, new CloudBusCallBack(null) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("the virtual router [uuid:%s] changed status to Disconnected",
                                        vrUuid));
                            } else {
                                logger.debug(String.format("the virtual router [uuid:%s] failed to changed status to Disconnected",
                                        vrUuid));
                            }
                        }
                    });
                }
            }
        });
    }
}
