package org.zstack.compute.vmscheduling;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.affinityGroup.AffinityGroupFilterFlow;
import org.zstack.compute.affinityGroup.AffinityGroupGlobalProperty;
import org.zstack.compute.affinityGroup.AffinityGroupManager;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.affinitygroup.AffinityGroupConstants;
import org.zstack.header.affinitygroup.AffinityGroupDeletionMsg;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.PaginateCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vmscheduling.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedulingRuleGroupBase {
    private static CLogger logger = Utils.getLogger(VmSchedulingRuleGroupBase.class);

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
    protected AffinityGroupFilterFlow affinityFilterFlow;
    @Autowired
    protected VmSchedulingRuleFilterFlow ruleFilterFlow;
    @Autowired
    protected AffinityGroupManager agMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager accr;

    protected VmSchedulingRuleGroupVO self;

    public VmSchedulingRuleGroupBase(VmSchedulingRuleGroupVO self) {
        this.self = self;
    }

    private String getSyncId() {
        return String.format("vm-scheduling-rule-group-%s", self.getUuid());
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VmSchedulingRuleReserveMsg) {
            handle((VmSchedulingRuleReserveMsg) msg);
        } else if (msg instanceof VmSchedulingRuleRollbackMsg) {
            handle((VmSchedulingRuleRollbackMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(VmSchedulingRuleRollbackMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmSchedulingRuleRollbackReply reply = new VmSchedulingRuleRollbackReply();
                String vmUuid = msg.getSpec().getVmInstance().getUuid();

                /* reset the hostUuid for vmInstance */
                SQL.New(VmInstanceVO.class).set(VmInstanceVO_.hostUuid, msg.getOriginHostUuid()).eq(VmInstanceVO_.uuid, vmUuid).update();

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(VmSchedulingRuleReserveMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmSchedulingRuleReserveReply reply = new VmSchedulingRuleReserveReply();
                String vmUuid = msg.getSpec().getVmInstance().getUuid();
                HostInventory host = msg.getHost();

                /* verify again the affinity */
                HostVO hostvo = dbf.findByUuid(host.getUuid(), HostVO.class);

                List<HostVO> hosts = ruleFilterFlow.filterHostCandidates(asList(hostvo), msg.getSpec(), msg.getVmSchedulingRuleGroupUuid());
                if (hosts.isEmpty()) {
                    reply.setError(operr("vm scheduling group[uuid:%s] reserve host [uuid:%s] for vm [uuid: %s] failed",
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
                return getSyncSignature();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateVmSchedulingRuleGroupMsg) {
            handle((APIUpdateVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIAddVmToVmSchedulingRuleGroupMsg) {
            handle((APIAddVmToVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIDetachVmFromVmSchedulingRuleGroupMsg) {
            handle((APIDetachVmFromVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIDeleteVmSchedulingRuleGroupMsg) {
            handle((APIDeleteVmSchedulingRuleGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteVmSchedulingRuleGroupMsg msg) {
        APIDeleteVmSchedulingRuleGroupEvent event = new APIDeleteVmSchedulingRuleGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteVmSchedulingRuleGroup(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void deleteVmSchedulingRuleGroup(APIDeleteVmSchedulingRuleGroupMsg msg, Completion completion) {
        FlowChain chain =  FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-vm-scheduling-rule-group-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("remove-vm-scheduling-rule-include-vm-group-%s", self.getUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        long count = SQL.New("select count(distinct rule.uuid) from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid = :groupUuid")
                                .param("groupUuid", msg.getUuid()).find();

                        SQL.New("select rule.uuid from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid = :groupUuid")
                                .param("groupUuid", msg.getUuid()).limit(1000).paginate(count, (List<String> ruleUuids, PaginateCompletion paginateCompletion) -> {
                                    new While<>(ruleUuids).each((ruleuuid, innerWhileCompletion) -> {
                                        AffinityGroupDeletionMsg affinityGroupDeletionMsg = new AffinityGroupDeletionMsg();
                                        affinityGroupDeletionMsg.setUuid(ruleuuid);
                                        bus.makeTargetServiceIdByResourceUuid(affinityGroupDeletionMsg, AffinityGroupConstants.SERVICE_ID, ruleuuid);
                                        bus.send(affinityGroupDeletionMsg, new CloudBusCallBack(innerWhileCompletion) {
                                            @Override
                                            public void run(MessageReply reply) {
                                                if(!reply.isSuccess()){
                                                    logger.debug(String.format("delete vm scheduling rule[uuid:%s] failed ", ruleuuid));
                                                }
                                                innerWhileCompletion.done();
                                            }
                                        });

                                    }).run(new WhileDoneCompletion(paginateCompletion) {
                                        @Override
                                        public void done(ErrorCodeList errorCodeList) {
                                            paginateCompletion.done();
                                        }
                                    });


                        }, new NoErrorCompletion() {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        dbf.removeByPrimaryKey(self.getUuid(), VmSchedulingRuleGroupVO.class);
                        completion.success();
                    }
                });
            }
        }).start();

    }


    private void handle(APIDetachVmFromVmSchedulingRuleGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIDetachVmFromVmSchedulingRuleGroupEvent evt = new APIDetachVmFromVmSchedulingRuleGroupEvent(msg.getId());
                agMgr.deleteAffinityGroupUsage(msg.getVmUuid());
                UpdateQuery.New(VmSchedulingRuleGroupRefVO.class)
                        .eq(VmSchedulingRuleGroupRefVO_.vmUuid, msg.getVmUuid())
                        .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, msg.getVmGroupUuid())
                        .hardDelete();

                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

    }

    private void handle(APIAddVmToVmSchedulingRuleGroupMsg msg) {
        APIAddVmToVmSchedulingRuleGroupEvent evt = new APIAddVmToVmSchedulingRuleGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                attachVmToVmSchedulingRuleGroup(msg, new Completion(chain) {
                    @Override
                    public void success() {
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
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private List<VmSchedulingRuleRefVO> getVmSchedulingRuleRef (String vmGroupUuid, String mode) {
        String sql = "select ref from VmSchedulingRuleRefVO ref, VmSchedulingRuleVO rule " +
                "where ref.vmSchedulingRuleUuid = rule.uuid and ref.vmGroupUuid =:vmGroupUuid";
        if (!StringUtils.isEmpty(mode)) {
            sql += " and rule.mode = :mode";
        }

        Query q = dbf.getEntityManager().createQuery(sql, VmSchedulingRuleRefVO.class);
        q.setParameter("vmGroupUuid", vmGroupUuid);
        if (!StringUtils.isEmpty(mode)) {
            q.setParameter("mode", VMSchedulingRuleMode.valueOf(mode));
        }

        return q.getResultList();
    }

    private void attachVmToVmSchedulingRuleGroup(APIAddVmToVmSchedulingRuleGroupMsg msg, Completion completion) {
        List<VmSchedulingRuleRefVO> refVOS = getVmSchedulingRuleRef(msg.getVmGroupUuid(), null);
        VmInstanceInventory vmInv = VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class));
        String hostUuid = getVmHostUuid(msg.getVmUuid());

        if (StringUtils.isEmpty(hostUuid)) {
            refVOS.stream().filter(ref -> StringUtils.isEmpty(ref.getHostGroupUuid())).forEach(ref -> {
                agMgr.createVmSystemTagForAffinityGroup(ref.getVmSchedulingRuleUuid(), msg.getVmUuid());
                agMgr.addVmToAffinityGroupUsage(ref.getVmSchedulingRuleUuid(), msg.getVmUuid());
            });

            VmSchedulingRuleGroupRefVO refVO = new VmSchedulingRuleGroupRefVO();
            refVO.setVmGroupUuid(msg.getVmGroupUuid());
            refVO.setVmUuid(msg.getVmUuid());
            dbf.persist(refVO);

            completion.success();
            return;
        }

        List<VmSchedulingRuleRefVO> hardRefvos = getVmSchedulingRuleRef(msg.getVmGroupUuid(), VMSchedulingRuleMode.HARD.toString());

        for (VmSchedulingRuleRefVO refVO : hardRefvos) {
            if (StringUtils.isEmpty(refVO.getHostGroupUuid())) {
                HostAllocatorSpec spec = new HostAllocatorSpec();
                spec.setVmInstance(vmInv);
                HostVO hostvo = dbf.findByUuid(hostUuid, HostVO.class);
                List<HostVO> hosts = affinityFilterFlow.filterHostCandidates(asList(hostvo), spec, refVO.getVmSchedulingRuleUuid());
                if (hosts.isEmpty()) {
                    completion.fail(operr("vm[uuid:%s] is now running on host[uuid:%s]," +
                            "which does not comply with the scheduling rule associated with vm scheduling group[uuid:%s].",
                            vmInv.getUuid(), hostUuid, refVO.getVmGroupUuid()));
                    return;
                }
                continue;
            }

            List<String> hostUuids = getHostGroupAttachHostUuids(refVO.getHostGroupUuid());
            if (hostUuids.isEmpty()) {
                completion.fail(operr("hostGroup[uuid:%s] is no host", msg.getVmGroupUuid()));
            }

            VmSchedulingRuleVO ruleVO = dbf.findByUuid(refVO.getVmSchedulingRuleUuid(), VmSchedulingRuleVO.class);
            if (ruleVO.getRule() == VMSchedulingRuleType.AFFINITY && !hostUuids.contains(hostUuid)) {
                completion.fail(operr("vm[uuid:%s] is now running on host[uuid:%s], " +
                                "which does not comply with the scheduling rule[%s] associated with vm scheduling group[uuid:%s].",
                        msg.getVmUuid(), hostUuid, VMSchedulingRuleType.AFFINITY.toString(), msg.getVmGroupUuid()));
                return;
            }

            if (ruleVO.getRule() == VMSchedulingRuleType.ANTIAFFINITY && hostUuids.contains(hostUuid) ) {
                completion.fail(operr("vm[uuid:%s] is now running on host[uuid:%s]," +
                                "which does not comply with the scheduling rule[%s] associated with vm scheduling group[uuid:%s].",
                         msg.getVmUuid(), hostUuid, VMSchedulingRuleType.ANTIAFFINITY.toString(), msg.getVmGroupUuid()));
                return;
            }

        }

        refVOS.stream().filter(ref -> StringUtils.isEmpty(ref.getHostGroupUuid())).forEach(ref -> {
            agMgr.createVmSystemTagForAffinityGroup(ref.getVmSchedulingRuleUuid(), msg.getVmUuid());
            agMgr.addVmToAffinityGroupUsage(ref.getVmSchedulingRuleUuid(), msg.getVmUuid());
        });

        VmSchedulingRuleGroupRefVO refVO = new VmSchedulingRuleGroupRefVO();
        refVO.setVmGroupUuid(msg.getVmGroupUuid());
        refVO.setVmUuid(msg.getVmUuid());
        dbf.persist(refVO);

        completion.success();
    }

    private List<String> getHostGroupAttachHostUuids(String hostGroupUuid) {
        return Q.New(HostSchedulingRuleGroupRefVO.class)
                .eq(HostSchedulingRuleGroupRefVO_.hostGroupUuid, hostGroupUuid)
                .select(HostSchedulingRuleGroupRefVO_.hostUuid)
                .listValues();
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


    private void handle(APIUpdateVmSchedulingRuleGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateVmSchedulingRuleGroupEvent event = new APIUpdateVmSchedulingRuleGroupEvent(msg.getId());
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

                event.setInventory(VmSchedulingRuleGroupInventory.valueOf(self));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

    }

}
