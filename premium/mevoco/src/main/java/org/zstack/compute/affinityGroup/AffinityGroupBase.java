package org.zstack.compute.affinityGroup;

import static java.util.Arrays.asList;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmInstanceManager;
import static org.zstack.core.Platform.operr;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.QueryMore;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.affinitygroup.APIAddVmToAffinityGroupEvent;
import org.zstack.header.affinitygroup.APIAddVmToAffinityGroupMsg;
import org.zstack.header.affinitygroup.APIChangeAffinityGroupStateEvent;
import org.zstack.header.affinitygroup.APIChangeAffinityGroupStateMsg;
import org.zstack.header.affinitygroup.APIDeleteAffinityGroupEvent;
import org.zstack.header.affinitygroup.APIDeleteAffinityGroupMsg;
import org.zstack.header.affinitygroup.APIGetCandidateVMForAttachingAffinityGroupMsg;
import org.zstack.header.affinitygroup.APIGetCandidateVMForAttachingAffinityGroupReply;
import org.zstack.header.affinitygroup.APIRemoveVmFromAffinityGroupEvent;
import org.zstack.header.affinitygroup.APIRemoveVmFromAffinityGroupMsg;
import org.zstack.header.affinitygroup.APIUpdateAffinityGroupEvent;
import org.zstack.header.affinitygroup.APIUpdateAffinityGroupMsg;
import org.zstack.header.affinitygroup.AffinityGroupDeletionMsg;
import org.zstack.header.affinitygroup.AffinityGroupDeletionReply;
import org.zstack.header.affinitygroup.AffinityGroupInventory;
import org.zstack.header.affinitygroup.AffinityGroupReserveMsg;
import org.zstack.header.affinitygroup.AffinityGroupReserveReply;
import org.zstack.header.affinitygroup.AffinityGroupRollbackMsg;
import org.zstack.header.affinitygroup.AffinityGroupRollbackReply;
import org.zstack.header.affinitygroup.AffinityGroupState;
import org.zstack.header.affinitygroup.AffinityGroupStateEvent;
import org.zstack.header.affinitygroup.AffinityGroupSystemTags;
import org.zstack.header.affinitygroup.AffinityGroupUsageInventory;
import org.zstack.header.affinitygroup.AffinityGroupUsageVO;
import org.zstack.header.affinitygroup.AffinityGroupUsageVO_;
import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AffinityGroupBase {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected AffinityGroupFilterFlow filterFlow;
    @Autowired
    protected AffinityGroupManager agMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager accr;

    protected AffinityGroupVO self;
    private static CLogger logger = Utils.getLogger(AffinityGroupBase.class);

    public AffinityGroupBase(AffinityGroupVO self) {
        this.self = self;
    }

    private String getSyncId() {
        return String.format("operate-affinity-group-%s", self.getUuid());
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg){
        if (msg instanceof AffinityGroupDeletionMsg) {
            handle((AffinityGroupDeletionMsg)msg);
        } else if (msg instanceof AffinityGroupReserveMsg) {
            handle((AffinityGroupReserveMsg)msg);
        } else if (msg instanceof AffinityGroupRollbackMsg) {
            handle((AffinityGroupRollbackMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteAffinityGroupMsg) {
            handle((APIDeleteAffinityGroupMsg)msg);
        } else if (msg instanceof APIUpdateAffinityGroupMsg){
            handle((APIUpdateAffinityGroupMsg)msg);
        } else if (msg instanceof APIAddVmToAffinityGroupMsg){
            handle((APIAddVmToAffinityGroupMsg)msg);
        } else if (msg instanceof APIRemoveVmFromAffinityGroupMsg){
            handle((APIRemoveVmFromAffinityGroupMsg)msg);
        } else if (msg instanceof APIChangeAffinityGroupStateMsg){
            handle((APIChangeAffinityGroupStateMsg)msg);
        } else if (msg instanceof APIGetCandidateVMForAttachingAffinityGroupMsg) {
            handle((APIGetCandidateVMForAttachingAffinityGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetCandidateVMForAttachingAffinityGroupMsg  msg) {
        List<String> vmUuids = Q.New(AffinityGroupUsageVO.class)
                .select(AffinityGroupUsageVO_.resourceUuid).listValues();

        QueryMore q = Q.New(VmInstanceVO.class, AccountResourceRefVO.class)
                .table0()
                    .selectThisTable()
                    .eq(VmInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped))
                    .eq(VmInstanceVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .table1()
                    .eq(AccountResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own);

        if  (!accr.isAdmin(msg.getSession())) {
            q.table1().eq(AccountResourceRefVO_.accountUuid, msg.getSession().getAccountUuid());
        }

        if (!CollectionUtils.isEmpty(vmUuids)) {
            q.table0().notIn(AccountVO_.uuid, vmUuids);
        }

        List<VmInstanceVO> results = q.list();
        List<VmInstanceVO> vos = new ArrayList<>();

        for (VmInstanceVO vo : results) {
            String hostUuid = getVmHostUuid(vo.getUuid());
            AffinityGroupInventory aginv = AffinityGroupInventory.valueOf(self);

            if (aginv.isHardPolicy() && hostUuid != null) {
                HostAllocatorSpec spec = new HostAllocatorSpec();
                VmInstanceInventory vmInv = VmInstanceInventory.valueOf(vo);
                spec.setVmInstance(vmInv);

                HostVO hostvo = dbf.findByUuid(hostUuid, HostVO.class);

                List<HostVO> hosts = filterFlow.filterHostCandidates(asList(hostvo), spec, self.getUuid());
                if (!hosts.isEmpty()) {
                    vos.add(vo);
                }
            } else {
                vos.add(vo);
            }
        }

        APIGetCandidateVMForAttachingAffinityGroupReply reply = new APIGetCandidateVMForAttachingAffinityGroupReply();
        reply.setInventories(VmInstanceInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    protected void handle(AffinityGroupDeletionMsg msg){
        AffinityGroupDeletionReply reply = new AffinityGroupDeletionReply();
        List<String> vmUuids = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.affinityGroupUuid, msg.getAffinityGroupUuid())
                .eq(AffinityGroupUsageVO_.resourceType, VmInstanceVO.class.getSimpleName()).select(AffinityGroupUsageVO_.resourceUuid).listValues();
        for (String vmUuid : vmUuids) {
            AffinityGroupSystemTags.AFFINITY_GROUP_UUID.deleteInherentTag(vmUuid);
        }

        pluginRgty.getExtensionList(DeleteAffinityGroupExtensionPoint.class).forEach(ext -> ext.beforeDeleteAffinityGroup(msg.getAffinityGroupUuid()));

        dbf.removeByPrimaryKey(msg.getAffinityGroupUuid(), AffinityGroupVO.class);
        bus.reply(msg, reply);
    }

    protected void handle(APIAddVmToAffinityGroupMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIAddVmToAffinityGroupEvent evt = new APIAddVmToAffinityGroupEvent(msg.getId());

                addVMToAffinityGroup(msg.getAffinityGroupUuid(), msg.getUuid(), new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(AffinityGroupInventory.valueOf(dbf.reload(self)));
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
                return String.format("api-add-vm-to-affinity-group-%s", msg.getUuid());
            }
        });
    }

    protected void handle(APIRemoveVmFromAffinityGroupMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIRemoveVmFromAffinityGroupEvent evt = new APIRemoveVmFromAffinityGroupEvent(msg.getId());

                removeVMFromAffinityGroup(msg.getAffinityGroupUuid(), msg.getUuid(), new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(AffinityGroupInventory.valueOf(dbf.reload(self)));
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
                return String.format("api-remove-vm-from-affinity-group-%s", msg.getUuid());
            }
        });
    }

    protected void handle(APIDeleteAffinityGroupMsg msg){
        final APIDeleteAffinityGroupEvent evt = new APIDeleteAffinityGroupEvent(msg.getId());
        final String issuer = AffinityGroupVO.class.getSimpleName();
        AffinityGroupInventory inv = AffinityGroupInventory.valueOf(self);
        final List<AffinityGroupInventory> ctx = Arrays.asList(inv);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-affinity-group-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
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
            }).then(new NoRollbackFlow() {
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
            chain.then(new NoRollbackFlow() {
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    protected void handle(APIUpdateAffinityGroupMsg msg) {
        APIUpdateAffinityGroupEvent evt = new APIUpdateAffinityGroupEvent(msg.getId());

        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        evt.setInventory(AffinityGroupInventory.valueOf(self));
        bus.publish(evt);
    }

    private void removeVMFromAffinityGroup(String affinityGroupUuid, String vmUuid, final Completion complete) {

        AffinityGroupSystemTags.AFFINITY_GROUP_UUID.deleteInherentTag(vmUuid);
        agMgr.deleteAffinityGroupUsage(vmUuid);
        pluginRgty.getExtensionList(VmRelationAffinityGroupExtensionPoint.class).forEach(ext -> ext.afterRemoveVmFromAffinityGroup(affinityGroupUuid, vmUuid));
        complete.success();
    }

    private String getVmHostUuid(String vmUuid) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (vm.getHostUuid() != null) {
            return vm.getHostUuid();
        }

        if (!AffinityGroupGlobalProperty.AFFINITY_GROUP_HOST_COUNT_ALL_VMS) {
            return null;
        }

        if (vm.getRootVolumeUuid() == null) {
            return null;
        }

        List<String> hostUuids = Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolumeUuid())
                .eq(LocalStorageResourceRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .select(LocalStorageResourceRefVO_.hostUuid).listValues();
        if (hostUuids == null || hostUuids.isEmpty()) {
            return null;
        }

        return hostUuids.get(0);
    }

    private void addVMToAffinityGroup(String affinityGroupUuid, String resourceUuid, final Completion complete) {
        AffinityGroupUsageVO vo = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.affinityGroupUuid, affinityGroupUuid).
                eq(AffinityGroupUsageVO_.resourceUuid, resourceUuid).find();
        if(vo != null){
            complete.fail(operr("VM [uuid: %s] has already been added to affinityGroup [uuid: %s]", resourceUuid, affinityGroupUuid));
            complete.success();
            return;
        }

        /* it will fail if it can not be allocated to this host */
        String hostUuid = getVmHostUuid(resourceUuid);
        AffinityGroupInventory aginv = AffinityGroupInventory.valueOf(dbf.findByUuid(affinityGroupUuid, AffinityGroupVO.class));
        if (aginv.isHardPolicy() && hostUuid != null) {
            HostAllocatorSpec spec = new HostAllocatorSpec();
            VmInstanceInventory vmInv = VmInstanceInventory.valueOf(dbf.findByUuid(resourceUuid, VmInstanceVO.class));
            spec.setVmInstance(vmInv);

            HostVO hostvo = dbf.findByUuid(hostUuid, HostVO.class);

            List<HostVO> hosts = filterFlow.filterHostCandidates(asList(hostvo), spec, affinityGroupUuid);
            if (hosts.isEmpty()) {
                complete.fail(operr("There are other VMs on this host [uuid: %s] belonging to same affinityGroup [%s]", hostUuid, affinityGroupUuid));
                return;
            }
        }

        agMgr.createVmSystemTagForAffinityGroup(affinityGroupUuid, resourceUuid);
        agMgr.addVmToAffinityGroupUsage(affinityGroupUuid, resourceUuid);

        pluginRgty.getExtensionList(VmRelationAffinityGroupExtensionPoint.class).forEach( ext -> ext.afterAddVmToAffinityGroup(affinityGroupUuid, resourceUuid));

        complete.success();
    }

    protected void handle(AffinityGroupReserveMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("host-allocator-reserve-flow-affinity-group-%s", msg.getAffinityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                AffinityGroupReserveReply reply = new AffinityGroupReserveReply();
                String vmUuid = msg.getSpec().getVmInstance().getUuid();
                HostInventory host = msg.getHost();

                /* verify again the affinity */
                HostVO hostvo = dbf.findByUuid(host.getUuid(), HostVO.class);

                List<HostVO> hosts = filterFlow.filterHostCandidates(asList(hostvo), msg.getSpec(), msg.getAffinityGroupUuid());
                if (hosts.isEmpty()) {
                    reply.setError(operr("affinityGroup [uuid:%s] reserve host [uuid:%s] for vm [uuid: %s] failed",
                            self.getUuid(), host.getUuid(), vmUuid));
                } else {
                    /* set vm host uuid to prevent other vm of same affinityGroup to be allocated to same host */
                    VmInstanceVO vo = dbf.findByUuid(vmUuid, VmInstanceVO.class);
                    reply.setOriginHostUuid(vo.getHostUuid());
                    SQL.New(VmInstanceVO.class).set(VmInstanceVO_.hostUuid, host.getUuid()).eq(VmInstanceVO_.uuid, vmUuid).update();
                }

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("host-allocator-reserve-flow-affinity-group-%s", msg.getAffinityGroupUuid());
            }
        });
    }

    protected void handle(AffinityGroupRollbackMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("host-allocator-reserve-flow-affinity-group-%s", msg.getAffinityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                AffinityGroupRollbackReply reply = new AffinityGroupRollbackReply();
                String vmUuid = msg.getSpec().getVmInstance().getUuid();

                /* reset the hostUuid for vmInstance */
                SQL.New(VmInstanceVO.class).set(VmInstanceVO_.hostUuid, msg.getOriginHostUuid()).eq(VmInstanceVO_.uuid, vmUuid).update();

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("host-allocator-rollback-flow-affinity-group-%s", msg.getAffinityGroupUuid());
            }
        });
    }

    protected void handle(APIChangeAffinityGroupStateMsg msg){
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIChangeAffinityGroupStateEvent evt = new APIChangeAffinityGroupStateEvent(msg.getId());

                final AffinityGroupState nextState = self.getState().nextState(AffinityGroupStateEvent.valueOf(msg.getStateEvent()));
                if (nextState == self.getState()) {
                    evt.setInventory(AffinityGroupInventory.valueOf(self));
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                /* verify whether all vms satisfy affinityGroup requirement */
                if (nextState == AffinityGroupState.Enabled) {
                    AffinityGroupInventory agInv = AffinityGroupInventory.valueOf(self);
                    for (AffinityGroupUsageInventory inv : agInv.getUsages()) {
                        if (!filterFlow.checkVmSatisfyAffinityGroup(inv.getResourceUuid(), self.getUuid())) {
                            evt.setError(operr("vm [uuid:%s] doesn't satisfy the affinityGroup [uuid:%s]", inv.getResourceUuid(), self.getUuid()));
                            bus.publish(evt);
                            chain.next();
                            return;
                        }
                    }
                }

                self.setState(nextState);
                self = dbf.updateAndRefresh(self);
                evt.setInventory(AffinityGroupInventory.valueOf(self));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("api-change-affinity-group-state-%s", msg.getUuid());
            }
        });
    }
}
