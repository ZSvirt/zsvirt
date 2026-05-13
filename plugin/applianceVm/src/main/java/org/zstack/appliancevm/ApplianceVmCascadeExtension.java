package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkDetachStruct;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageDetachStruct;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.toMap;

/**
 */
public class ApplianceVmCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ApplianceVmCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ThreadFacade thdf;

    private static String NAME = ApplianceVmVO.class.getSimpleName();


    protected static final int OP_NOPE = 0;
    protected static final int OP_MIGRATE = 1;
    protected static final int OP_DELETION = 2;

    private static final List<String> ignoreReleaseResourceFailIssuers = Arrays.asList(HostVO.class.getSimpleName(), PrimaryStorageVO.class.getSimpleName());

    protected int toDeleteOpCode(CascadeAction action) {
        if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                return OP_DELETION;
            } else {
                return OP_MIGRATE;
            }
        }

        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer()) && IpRangeVO.class.getSimpleName().equals(action.getRootIssuer())) {
            return OP_DELETION;
        }

        if (ApplianceVmVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        return OP_NOPE;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            handlePrimaryStorageDetach(action, completion);
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            handleL2NetworkDetach(action, completion);
        } else {
            completion.success();
        }
    }

    @Transactional(readOnly = true)
    private List<VmInstanceVO> getVmFromL2NetworkDetached(List<L2NetworkDetachStruct> structs) {
        Set<VmInstanceVO> apvms = new HashSet<>();
        for (L2NetworkDetachStruct s : structs) {
            String sql = "select vm" +
                    " from VmInstanceVO vm, L2NetworkVO l2, L3NetworkVO l3, VmNicVO nic" +
                    " where vm.type = :vmType" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.state in (:vmStates)" +
                    " and vm.uuid = nic.vmInstanceUuid" +
                    " and nic.l3NetworkUuid = l3.uuid" +
                    " and l3.l2NetworkUuid = l2.uuid" +
                    " and l2.uuid = :l2Uuid";
            TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            q.setParameter("vmType", ApplianceVmConstant.APPLIANCE_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Running,
                    VmInstanceState.Migrating,
                    VmInstanceState.Starting,
                    VmInstanceState.Rebooting));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("l2Uuid", s.getL2NetworkUuid());
            apvms.addAll(q.getResultList());
        }

        List<VmInstanceVO> ret = new ArrayList<>(apvms.size());
        ret.addAll(apvms);
        return ret;
    }

    private void migrateOrStopVmOnClusterDetach(final List<VmInstanceVO> toMigrate,
                                                List<String> clusterUuids,
                                                final Completion completion) {
        final List<String> avoidHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .listValues();
        final List<VmInstanceVO> toDelete = new ArrayList<>();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("handle-appliance-vm-for-cluster-detach"));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                final List<MigrateVmMsg> migrateVmMsgs = CollectionUtils.transformAndRemoveNull(toMigrate, arg -> {
                    MigrateVmMsg msg = new MigrateVmMsg();
                    msg.setVmInstanceUuid(arg.getUuid());
                    msg.setAvoidHostUuids(avoidHostUuids);
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                    return msg;
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "migrate-appliance-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bus.send(migrateVmMsgs, 2, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        VmInstanceVO apvm = toMigrate.get(replies.indexOf(r));
                                        toDelete.add(apvm);
                                        logger.warn(String.format("failed to migrate appliance vm[uuid:%s, name:%s], %s. will try to delete it", apvm.getUuid(), r.getError(), apvm.getName()));
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-appliance-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (toDelete.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<VmInstanceDeletionMsg> msgs = CollectionUtils.transformAndRemoveNull(toDelete, arg -> {
                            VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                            msg.setIgnoreResourceReleaseFailure(true);
                            msg.setVmInstanceUuid(arg.getUuid());
                            msg.setTimeout(TimeUnit.SECONDS.toMillis(ApplianceVmGlobalConfig.DELETE_TIMEOUT.value(Long.class)));
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                            return msg;
                        });

                        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        VmInstanceVO apvm = toDelete.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to delete vm[uuid:%s] for cluster detached, %s. However, detaching will go on", apvm.getUuid(), r.getError()));
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();

    }

    private void handleL2NetworkDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        List<VmInstanceVO> apvms = getVmFromL2NetworkDetached(structs);
        if (apvms.isEmpty()) {
            completion.success();
            return;
        }

        List<String> l3uuids = new ArrayList<>();
        for (L2NetworkDetachStruct s : structs) {
            List<String> ret = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, s.getL2NetworkUuid()).select(L3NetworkVO_.uuid).listValues();
            if (!ret.isEmpty()) {
                l3uuids.addAll(ret);
            }
        }

        List<ApplianceVmVO> applianceVmVOS = Q.New(ApplianceVmVO.class).in(ApplianceVmVO_.uuid, apvms.stream()
                .map(VmInstanceVO::getUuid).collect(Collectors.toList())).list();

        if (!applianceVmVOS.isEmpty()) {
            for (ApvmCascadeFilterExtensionPoint ext : pluginRgty.getExtensionList(ApvmCascadeFilterExtensionPoint.class)) {
                applianceVmVOS = ext.filterApplianceVmCascade(applianceVmVOS, action, action.getParentIssuer(), l3uuids,
                        new ArrayList<>(), new ArrayList<>());
            }
        }

        if (applianceVmVOS.isEmpty()) {
            completion.success();
            return;
        }

        List<String> needMigrateVmUuids = applianceVmVOS.stream().map(ApplianceVmVO::getUuid).collect(Collectors.toList());
        apvms = apvms.stream().filter(apvm -> needMigrateVmUuids.contains(apvm.getUuid())).collect(Collectors.toList());

        List<String> clusterUuids = CollectionUtils.transformAndRemoveNull(structs, L2NetworkDetachStruct::getClusterUuid);

        migrateOrStopVmOnClusterDetach(apvms, clusterUuids, completion);
    }

    @Transactional(readOnly = true)
    private List<VmInstanceVO> getVmForPrimaryStorageDetached(List<PrimaryStorageDetachStruct> structs) {
        Set<VmInstanceVO> vms = new HashSet<>();
        for (PrimaryStorageDetachStruct s : structs) {
            String sql = "select vm" +
                    " from VmInstanceVO vm, PrimaryStorageVO ps, VolumeVO vol" +
                    " where vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.uuid = vol.vmInstanceUuid" +
                    " and vol.primaryStorageUuid = :psUuid";
            TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            q.setParameter("vmType", ApplianceVmConstant.APPLIANCE_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Running,
                    VmInstanceState.Starting,
                    VmInstanceState.Migrating,
                    VmInstanceState.Rebooting));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("psUuid", s.getPrimaryStorageUuid());
            vms.addAll(q.getResultList());
        }

        List<VmInstanceVO> ret = new ArrayList<>(vms.size());
        ret.addAll(vms);
        return ret;
    }

    private void handlePrimaryStorageDetach(CascadeAction action, final Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        final List<VmInstanceVO> vmInstanceVOs = getVmForPrimaryStorageDetached(structs);
        if (vmInstanceVOs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> clusterUuids = CollectionUtils.transformAndRemoveNull(structs, PrimaryStorageDetachStruct::getClusterUuid);

        migrateOrStopVmOnClusterDetach(vmInstanceVOs, clusterUuids, completion);
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        String syncSignature = String.format("%s eo cleanup", ApplianceVmVO.class.getSimpleName());
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                dbf.eoCleanup(ApplianceVmVO.class);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return syncSignature;
            }
        });
    }

    protected Flow returnUsedIpFlow() {
        return new NoRollbackFlow() {

            String __name__ = "delete-ip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<UsedIpInventory> toDeleteIps = (List<UsedIpInventory>)
                        data.get(VmInstanceConstant.Params.UsedIPInventory.toString());

                if (toDeleteIps.isEmpty()) {
                    logger.debug("no ip need to delete");
                    trigger.next();
                    return;
                }

                List<ReturnIpMsg> dmsgs = new ArrayList<>();
                for (UsedIpInventory ip : toDeleteIps) {
                    ReturnIpMsg msg = new ReturnIpMsg();
                    msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                    msg.setUsedIpUuid(ip.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                    dmsgs.add(msg);
                }

                List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                new While<>(dmsgs).step((deleteMsg, whileCompletion) -> {
                    bus.send(deleteMsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format("delete ip [uuid:%s] failed %s", deleteMsg.getUsedIpUuid(),
                                        reply.getError().getDetails()));
                            }
                            whileCompletion.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    protected Flow deleteVmNicFlow() {
        return new NoRollbackFlow() {
            String __name__ = "delete-vmnic";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VmNicInventory > toDeleteNics = (List<VmNicInventory>)
                        data.get(VmInstanceConstant.Params.VmNicInventory.toString());

                if(toDeleteNics.isEmpty()) {
                    logger.debug(String.format("no nic need for delete"));
                    trigger.next();
                    return;
                }

                List<DetachNicFromVmMsg> dmsgs = new ArrayList<>();
                for (VmNicInventory nic : toDeleteNics) {
                    DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                    msg.setVmNicUuid(nic.getUuid());
                    msg.setVmInstanceUuid(nic.getVmInstanceUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
                    dmsgs.add(msg);
                }

                List<ErrorCode> errorCodes = Collections.synchronizedList(new LinkedList<ErrorCode>());
                new While<>(dmsgs).step((deleteMsg, whileCompletion) -> {
                    bus.send(deleteMsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errorCodes.add(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodes.isEmpty()) {
                            logger.debug(String.format("detach nic for " +
                                    "delete l3 success"));
                            trigger.next();
                            return;
                        }
                        trigger.fail(errorCodes.get(0));
                    }
                });
            }
        };
    }

    protected void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = toDeleteOpCode(action);

        if (op == OP_NOPE) {
            completion.success();
            return;
        }
        List<VmNicInventory> toDeleteNics = new ArrayList<>();
        List<UsedIpInventory> toDeleteIps = new ArrayList<>();

        final List<ApplianceVmInventory> apvms = apvmFromDeleteAction(action, toDeleteNics, toDeleteIps);
        if (apvms == null) {
            logger.debug("no apvms to delete");
            completion.success();
            return;
        }

        final List<ApplianceVmInventory> apvmToMigrate = new ArrayList<ApplianceVmInventory>();
        final List<ApplianceVmInventory> apvmToDelete = new ArrayList<ApplianceVmInventory>();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-cascade-for-appliance-vm");
        chain.getData().put(VmInstanceConstant.Params.UsedIPInventory.toString(), toDeleteIps);
        chain.getData().put(VmInstanceConstant.Params.VmNicInventory.toString(), toDeleteNics);

        if (op == OP_MIGRATE) {
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    for (ApplianceVmInventory apvm : apvms) {
                        if (VmInstanceState.Running.toString().equals(apvm.getState())) {
                            apvmToMigrate.add(apvm);
                        } else {
                            apvmToDelete.add(apvm);
                        }
                    }

                    List<String> avoidHostUuids = null;
                    if (action.getRootIssuer().equals(ClusterVO.class.getSimpleName())) {
                        List<ClusterInventory> clusters = action.getRootIssuerContext();
                        List<String> clusterUuids = CollectionUtils.transformAndRemoveNull(clusters, ClusterInventory::getUuid);
                        avoidHostUuids = Q.New(HostVO.class)
                                .select(HostVO_.uuid)
                                .in(HostVO_.clusterUuid, clusterUuids)
                                .listValues();
                    }
                    final List<String> finalAvoidHostUuids = avoidHostUuids;

                    if (!apvmToMigrate.isEmpty()) {
                        flow(new NoRollbackFlow() {
                            String __name__ = "try-migrate-appliancevm";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                final List<GetVmMigrationTargetHostMsg> gmsgs = CollectionUtils.transformAndRemoveNull(apvmToMigrate, arg -> {
                                    GetVmMigrationTargetHostMsg gmsg = new GetVmMigrationTargetHostMsg();
                                    gmsg.setVmInstanceUuid(arg.getUuid());
                                    if (finalAvoidHostUuids != null) {
                                        gmsg.setAvoidHostUuids(finalAvoidHostUuids);
                                    }
                                    bus.makeTargetServiceIdByResourceUuid(gmsg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                    return gmsg;
                                });

                                bus.send(gmsgs, 1, new CloudBusListCallBack(trigger) {
                                    @Override
                                    public void run(List<MessageReply> replies) {
                                        List<ApplianceVmInventory> apvmCannotMigrate = new ArrayList<ApplianceVmInventory>();
                                        for (MessageReply reply : replies) {
                                            if (!reply.isSuccess() || ((GetVmMigrationTargetHostReply) reply).getHosts().isEmpty()) {
                                                ApplianceVmInventory apvm = apvmToMigrate.get(replies.indexOf(reply));
                                                apvmCannotMigrate.add(apvm);
                                            }
                                        }

                                        apvmToMigrate.removeAll(apvmCannotMigrate);
                                        apvmToDelete.addAll(apvmCannotMigrate);
                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "migrate-appliancevm";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                if (apvmToMigrate.isEmpty()) {
                                    trigger.next();
                                    return;
                                }

                                List<MigrateVmMsg> mmsgs = CollectionUtils.transformAndRemoveNull(apvmToMigrate, arg -> {
                                    MigrateVmMsg mmsg = new MigrateVmMsg();
                                    mmsg.setVmInstanceUuid(arg.getUuid());
                                    mmsg.setAvoidHostUuids(finalAvoidHostUuids);
                                    bus.makeTargetServiceIdByResourceUuid(mmsg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                    return mmsg;
                                });

                                bus.send(mmsgs, 2, new CloudBusListCallBack(trigger) {
                                    @Override
                                    public void run(List<MessageReply> replies) {
                                        for (MessageReply r : replies) {
                                            if (!r.isSuccess()) {
                                                ApplianceVmInventory apvm = apvmToMigrate.get(replies.indexOf(r));
                                                apvmToDelete.add(apvm);
                                            }
                                        }

                                        trigger.next();
                                    }
                                });
                            }
                        });
                    }
                }
            });

        } else if (op == OP_DELETION) {
            apvmToDelete.addAll(apvms);
        }

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(returnUsedIpFlow());
                flow(deleteVmNicFlow());

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-appliancevm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (apvmToDelete.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        boolean ignoreResourceReleaseFailure = ignoreReleaseResourceFailIssuers.contains(action.getParentIssuer()) ||
                                ignoreReleaseResourceFailIssuers.contains(action.getRootIssuer());

                        List<VmInstanceDeletionMsg> msgs = CollectionUtils.transformAndRemoveNull(apvmToDelete, arg -> {
                            VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                            msg.setIgnoreResourceReleaseFailure(ignoreResourceReleaseFailure);
                            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
                            msg.setVmInstanceUuid(arg.getUuid());
                            msg.setTimeout(TimeUnit.SECONDS.toMillis(ApplianceVmGlobalConfig.DELETE_TIMEOUT.value(Long.class)));
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                            return msg;
                        });

                        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            trigger.fail(r.getError());
                                            return;
                                        }
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(HostVO.class.getSimpleName(), L3NetworkVO.class.getSimpleName(),
                IpRangeVO.class.getSimpleName(), PrimaryStorageVO.class.getSimpleName(), L2NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Transactional
    protected List<ApplianceVmInventory> apvmFromDeleteAction(CascadeAction action, List<VmNicInventory> toDeleteNics,
                                                              List<UsedIpInventory> toDeleteIps) {
        /*
        * return null or non-empty list
        * */
        List<ApplianceVmInventory> ret = new ArrayList<>();

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> huuids = CollectionUtils.transformAndRemoveNull(hosts, HostInventory::getUuid);

            List<ApplianceVmVO> lst = Q.New(ApplianceVmVO.class)
                    .in(ApplianceVmVO_.hostUuid, huuids)
                    .list();
            Map<String, ApplianceVmVO> vmvos = toMap(lst, ApplianceVmVO::getUuid, Function.identity());

            if (ClusterVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ClusterInventory> clusters = action.getRootIssuerContext();
                List<String> clusterUuids = CollectionUtils.transformAndRemoveNull(clusters, ClusterInventory::getUuid);
                lst = Q.New(ApplianceVmVO.class)
                        .in(ApplianceVmVO_.clusterUuid, clusterUuids)
                        .list();
                for (ApplianceVmVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            } else if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ZoneInventory> zones = action.getRootIssuerContext();
                List<String> zoneUuids = CollectionUtils.transformAndRemoveNull(zones, ZoneInventory::getUuid);
                lst = Q.New(ApplianceVmVO.class)
                        .in(ApplianceVmVO_.zoneUuid, zoneUuids)
                        .list();
                for (ApplianceVmVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            }

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos.values());
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> pruuids = CollectionUtils.transformAndRemoveNull(action.getParentIssuerContext(), PrimaryStorageInventory::getUuid);

            List<ApplianceVmVO> vmvos = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VolumeVO vol, PrimaryStorageVO pr where vm.uuid = vol.vmInstanceUuid" +
                            " and vol.primaryStorageUuid = pr.uuid and vol.type = :volType and pr.uuid in (:uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("uuids", pruuids);
                    q.setParameter("volType", VolumeType.Root);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos);
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L3NetworkInventory> l3s = action.getParentIssuerContext();
            List<String> l3uuids = CollectionUtils.transformAndRemoveNull(l3s, L3NetworkInventory::getUuid);

            String sql = "select apvm from ApplianceVmVO apvm where apvm.uuid in (select nic.vmInstanceUuid from VmNicVO nic where nic.l3NetworkUuid in (:l3Uuids))";
            TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
            q.setParameter("l3Uuids", l3uuids);
            List<ApplianceVmVO> apvms = q.getResultList();
            if (!apvms.isEmpty()) {
                for (ApvmCascadeFilterExtensionPoint ext : pluginRgty.getExtensionList(ApvmCascadeFilterExtensionPoint.class)) {
                    apvms = ext.filterApplianceVmCascade(apvms, action, action.getParentIssuer(), l3uuids,
                            toDeleteNics, toDeleteIps);
                }
                if (!apvms.isEmpty()) {
                    ret = ApplianceVmInventory.valueOf1(apvms);
                }
            }
        } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<IpRangeInventory> ipRanges = action.getParentIssuerContext();
            if (ipRanges.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> ipRangeUuidList = CollectionUtils.transform(ipRanges, IpRangeInventory::getUuid);
            final List<String> ipruuids = Q.New(NormalIpRangeVO.class)
                    .select(NormalIpRangeVO_.uuid)
                    .in(NormalIpRangeVO_.uuid, ipRangeUuidList)
                    .listValues();

            if (ipruuids.isEmpty()) {
                return new ArrayList<>();
            }

            /* find out applianceVm which nic ip address is in the ip range() */
            List<ApplianceVmVO> vmvos = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VmNicVO nic, UsedIpVO ip, NormalIpRangeVO ipr where vm.uuid = nic.vmInstanceUuid" +
                            " and nic.uuid = ip.vmNicUuid and ip.ipRangeUuid = ipr.uuid and ipr.uuid in (:uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("uuids", ipruuids);
                    return q.getResultList();
                }
            }.call();

            // find out appliance vm whose ip is gateway of ip range (only private network match this selection)

            final List<String> iprL3Uuids = CollectionUtils.transformAndRemoveNull(ipRanges, IpRangeInventory::getL3NetworkUuid);
            List<ApplianceVmVO> vmvos1 = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid in (:l3uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("l3uuids", iprL3Uuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos1.isEmpty()) {
                for (final IpRangeInventory ipr : ipRanges) {
                    for (ApplianceVmVO vm : vmvos1) {
                        for (VmNicVO nic : vm.getVmNics()) {
                            if (ipr.getGateway().equals(nic.getIp())) {
                                vmvos.add(vm);
                            }
                        }
                    }
                }
            }
            for (ApvmCascadeFilterExtensionPoint ext : pluginRgty.getExtensionList(ApvmCascadeFilterExtensionPoint.class)) {
                vmvos = ext.filterApplianceVmCascade(vmvos, action, action.getParentIssuer(), ipruuids,
                        toDeleteNics, toDeleteIps);
            }

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos);
            }
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<AccountInventory> parentIssuerContext = action.getParentIssuerContext();
            final List<String> auuids = CollectionUtils.transformAndRemoveNull(parentIssuerContext, AccountInventory::getUuid);

            List<ApplianceVmVO> vos = ResourceHelper.findOwnResources(ApplianceVmVO.class, auuids);
            vos.removeIf(vo -> !Objects.equals(vo.getResourceType(), "VpcRouterVmVO"));

            if (!vos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            int op = toDeleteOpCode(action);
            if (op != OP_NOPE) {
                List<ApplianceVmInventory> apvms = apvmFromDeleteAction(action, new ArrayList<>(),
                        new ArrayList<>());

                if (!apvms.isEmpty()) {
                    return action.copy().setParentIssuer(NAME).setParentIssuerContext(apvms);
                }
            }
        }

        return null;
    }
}
