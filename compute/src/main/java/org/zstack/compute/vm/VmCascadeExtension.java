package org.zstack.compute.vm;

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
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkDetachStruct;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageDetachStruct;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.GetVmUuidFromShareableVolumeExtensionPoint;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VmCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VmCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VmInstanceDeletionPolicyManager deletionPolicyManager;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final String NAME = VmInstanceVO.class.getSimpleName();

    protected static final int OP_NOPE = 0;
    protected static final int OP_STOP = 1;
    protected static final int OP_DELETION = 2;
    private static final int OP_REMOVE_INSTANCE_OFFERING = 3;
    protected static final int OP_DETACH_NIC = 4;

    protected int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                return OP_DELETION;
            } else {
                return OP_STOP;
            }
        }

        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH_NIC;
        }

        if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH_NIC;
        }

        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (InstanceOfferingVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_REMOVE_INSTANCE_OFFERING;
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
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
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE) ||
                action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_FROM_HOST_CODE)) {
            handleL2NetworkDetach(action, completion);
        } else {
            completion.success();
        }
    }

    class VmNicDetachResult {
        List<DetachNicFromVmMsg> dmsgs;
    }

    @Transactional(readOnly = true)
    private VmNicDetachResult getVmNicDetachMsgs(List<L2NetworkDetachStruct> structs) {
        List<DetachNicFromVmMsg> dmsgs  = new ArrayList<>();

        for (L2NetworkDetachStruct s : structs) {
            String sql = "select vm.uuid, nic.uuid from VmInstanceVO vm, VmNicVO nic, L3NetworkVO l3"
                    + " where l3.l2NetworkUuid = :l2NetworkUuid"
                    + " and nic.l3NetworkUuid = l3.uuid"
                    + " and nic.vmInstanceUuid = vm.uuid";

            TypedQuery<Tuple> q;
            if (s.getClusterUuid() != null) {
                sql += " and vm.clusterUuid = :clusterUuid";
                q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("clusterUuid", s.getClusterUuid());
            } else {
                sql += " and (vm.hostUuid = :hostUuid or (vm.hostUuid is null and vm.lastHostUuid = :hostUuid))";
                q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("hostUuid", s.getHostUuid());
            }

            q.setParameter("l2NetworkUuid", s.getL2NetworkUuid());
            List<Tuple> result = q.getResultList();
            Map<String, String> nicVmMap = new HashMap<>();
            for (Tuple t : result) {
                String vmUuid = t.get(0, String.class);
                String nicUuid = t.get(1, String.class);
                nicVmMap.put(nicUuid, vmUuid);
            }

            for (Map.Entry<String, String> entry : nicVmMap.entrySet()) {
                String nicUuid = entry.getKey();
                String vmUuid = entry.getValue();
                DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                msg.setVmInstanceUuid(vmUuid);
                msg.setVmNicUuid(nicUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                dmsgs.add(msg);
            }
        }

        VmNicDetachResult result = new VmNicDetachResult();
        result.dmsgs = dmsgs;
        return result;
    }

    private void detachVmNicCascade(List<DetachNicFromVmMsg> msgs, boolean deleteFromDb, final Completion completion) {
        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        int parallelism = 10;
        List<String> vmNicUuids = new ArrayList<String>();
        new While<>(msgs).step((msg, compl) -> bus.send(msg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to detach nic[uuid:%s] from the vm[uuid:%s], %s",
                            msg.getVmNicUuid(), msg.getVmInstanceUuid(), reply.getError()));
                } else {
                    vmNicUuids.add(msg.getVmNicUuid());
                }
                compl.done();
            }
        }), parallelism).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!vmNicUuids.isEmpty() && deleteFromDb) {
                    SQL.New(AccountResourceRefVO.class)
                            .in(AccountResourceRefVO_.resourceUuid, vmNicUuids)
                            .eq(AccountResourceRefVO_.resourceType, VmNicVO.class.getSimpleName())
                            .delete();
                }

                completion.success();
            }
        });
    }

    private void handleL2NetworkDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        final VmNicDetachResult result = getVmNicDetachMsgs(structs);

        detachVmNicCascade(result.dmsgs, true, completion);
    }

    @Transactional(readOnly = true)
    private List<String> getVmUuidForPrimaryStorageDetached(List<PrimaryStorageDetachStruct> structs) {
        List<String> vmUuids = new ArrayList<>();
        for (PrimaryStorageDetachStruct s : structs) {
            String sql = "select distinct vm.uuid" +
                    " from VmInstanceVO vm, VolumeVO vol" +
                    " where vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.uuid = vol.vmInstanceUuid" +
                    " and vol.primaryStorageUuid = :psUuid";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Unknown,
                    VmInstanceState.Running,
                    VmInstanceState.Pausing,
                    VmInstanceState.Paused));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("psUuid", s.getPrimaryStorageUuid());
            vmUuids.addAll(q.getResultList());
            //Get the vmuuid attached by the ShareableVolume
            for (GetVmUuidFromShareableVolumeExtensionPoint exp : pluginRgty.getExtensionList(GetVmUuidFromShareableVolumeExtensionPoint.class)) {
                vmUuids.addAll(exp.getVmUuidFromShareableVolumeByPrimaryStorage(s));
            }
        }

        return vmUuids;
    }

    private void handlePrimaryStorageDetach(CascadeAction action, final Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        final List<String> vmUuids = getVmUuidForPrimaryStorageDetached(structs);
        if (vmUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<StopVmInstanceMsg> msgs = transformAndRemoveNull(vmUuids, arg -> {
            StopVmInstanceMsg msg = new StopVmInstanceMsg();
            msg.setVmInstanceUuid(arg);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg);
            return msg;
        });

        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        String vmUuid = vmUuids.get(replies.indexOf(r));
                        logger.warn(String.format("failed to stop vm[uuid:%s] for primary storage detached, %s." +
                                " However, detaching will go on", vmUuid, r.getError()));
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VmInstanceVO.class);
        completion.success();
    }

    private void cleanupDeletedTemplatedVmInstanceRows(Collection<String> vmUuids) {
        cleanupDeletedTemplatedVmInstanceRows(vmUuids, vmUuids);
    }

    private void cleanupDeletedTemplatedVmInstanceRows(Collection<String> deletedVmUuids, Collection<String> deletingVmUuids) {
        Set<String> successfullyDeletedVmUuids = new HashSet<>(deletedVmUuids);
        successfullyDeletedVmUuids.remove(null);
        if (successfullyDeletedVmUuids.isEmpty()) {
            return;
        }

        List<String> templatedVmUuids = Q.New(TemplatedVmInstanceVO.class)
                .select(TemplatedVmInstanceVO_.uuid)
                .in(TemplatedVmInstanceVO_.uuid, successfullyDeletedVmUuids)
                .listValues();
        List<String> fullyDeletedTemplatedVmUuids =
                findFullyDeletedTemplatedVmInstanceUuids(successfullyDeletedVmUuids, deletingVmUuids, templatedVmUuids);
        if (!fullyDeletedTemplatedVmUuids.isEmpty()) {
            cleanupTemplatedVmInstanceRows(fullyDeletedTemplatedVmUuids);
        }

        SQL.New(TemplatedVmInstanceCacheVO.class)
                .in(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid, successfullyDeletedVmUuids)
                .hardDelete();
        SQL.New(TemplatedVmInstanceRefVO.class)
                .in(TemplatedVmInstanceRefVO_.vmInstanceUuid, successfullyDeletedVmUuids)
                .hardDelete();
    }

    private List<String> findFullyDeletedTemplatedVmInstanceUuids(Set<String> successfullyDeletedVmUuids,
                                                                  Collection<String> deletingVmUuids,
                                                                  List<String> templatedVmUuids) {
        if (templatedVmUuids.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> attemptedVmUuids = new HashSet<>(deletingVmUuids);
        attemptedVmUuids.remove(null);

        Map<String, Set<String>> cacheVmUuidsByTemplateUuid = new HashMap<>();
        List<Tuple> cacheTuples = Q.New(TemplatedVmInstanceCacheVO.class)
                .select(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid)
                .in(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, templatedVmUuids)
                .listTuple();
        for (Tuple t : cacheTuples) {
            addVmUuidMapping(cacheVmUuidsByTemplateUuid, t.get(0, String.class), t.get(1, String.class));
        }

        Map<String, Set<String>> refVmUuidsByTemplateUuid = new HashMap<>();
        List<Tuple> refTuples = Q.New(TemplatedVmInstanceRefVO.class)
                .select(TemplatedVmInstanceRefVO_.templatedVmInstanceUuid, TemplatedVmInstanceRefVO_.vmInstanceUuid)
                .in(TemplatedVmInstanceRefVO_.templatedVmInstanceUuid, templatedVmUuids)
                .listTuple();
        for (Tuple t : refTuples) {
            addVmUuidMapping(refVmUuidsByTemplateUuid, t.get(0, String.class), t.get(1, String.class));
        }

        return templatedVmUuids.stream()
                .filter(uuid -> areAllRelatedVmDeletionsSuccessful(
                        cacheVmUuidsByTemplateUuid.get(uuid), successfullyDeletedVmUuids))
                .filter(uuid -> areAttemptedRelatedVmDeletionsSuccessful(
                        refVmUuidsByTemplateUuid.get(uuid), attemptedVmUuids, successfullyDeletedVmUuids))
                .collect(Collectors.toList());
    }

    private void addVmUuidMapping(Map<String, Set<String>> vmUuidsByTemplateUuid, String templateUuid, String vmUuid) {
        if (templateUuid == null || vmUuid == null) {
            return;
        }

        vmUuidsByTemplateUuid.computeIfAbsent(templateUuid, k -> new HashSet<>()).add(vmUuid);
    }

    private boolean areAllRelatedVmDeletionsSuccessful(Collection<String> relatedVmUuids,
                                                       Set<String> successfullyDeletedVmUuids) {
        if (relatedVmUuids == null || relatedVmUuids.isEmpty()) {
            return true;
        }

        return successfullyDeletedVmUuids.containsAll(relatedVmUuids);
    }

    private boolean areAttemptedRelatedVmDeletionsSuccessful(Collection<String> relatedVmUuids,
                                                            Set<String> attemptedVmUuids,
                                                            Set<String> successfullyDeletedVmUuids) {
        if (relatedVmUuids == null || relatedVmUuids.isEmpty()) {
            return true;
        }

        for (String relatedVmUuid : relatedVmUuids) {
            if (attemptedVmUuids.contains(relatedVmUuid) && !successfullyDeletedVmUuids.contains(relatedVmUuid)) {
                return false;
            }
        }

        return true;
    }

    private void cleanupTemplatedVmInstanceRows(List<String> templatedVmUuids) {
        SQL.New(TemplatedVmInstanceCacheVO.class)
                .in(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, templatedVmUuids)
                .hardDelete();
        SQL.New(TemplatedVmInstanceRefVO.class)
                .in(TemplatedVmInstanceRefVO_.templatedVmInstanceUuid, templatedVmUuids)
                .hardDelete();
        SQL.New(TemplatedVmInstanceVO.class)
                .in(TemplatedVmInstanceVO_.uuid, templatedVmUuids)
                .hardDelete();
    }

    protected List<DetachNicFromVmMsg> handleDeletionForIpRange(List<VmDeletionStruct> vminvs, List<IpRangeInventory> iprs) {
        List<DetachNicFromVmMsg> msgs = new ArrayList<>();
        List<String> uuids = iprs.stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        for (VmDeletionStruct vm : vminvs) {
            for (VmNicInventory nic : vm.getInventory().getVmNics()) {
                UsedIpInventory ip4 = null, ip6 = null;
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    if (ip.getIpVersion() == IPv6Constants.IPv4) {
                        ip4 = ip;
                    } else if (ip.getIpVersion() == IPv6Constants.IPv6) {
                        ip6 = ip;
                    }
                }

                /* both ipv4 and ipv6 has been deleted or deleting, then delete nic */
                if ((ip4 == null || uuids.contains(ip4.getIpRangeUuid()))
                        && (ip6 == null || uuids.contains(ip6.getIpRangeUuid()))) {
                    L3NetworkVO l3VO = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
                    if (!l3VO.getEnableIPAM()) {
                        continue;
                    }

                    DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                    msg.setVmInstanceUuid(nic.getVmInstanceUuid());
                    msg.setVmNicUuid(nic.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getInventory().getUuid());
                    msgs.add(msg);
                } else {
                    boolean deleteIp4 = false, hasIp6 = false;
                    if (ip4 != null && uuids.contains(ip4.getIpRangeUuid())) {
                        deleteIp4 = true;
                    }
                    if (ip6 != null && !uuids.contains(ip6.getIpRangeUuid())) {
                        hasIp6 = true;
                    }

                    /* ipv4 is deleted and ipv6 is not deleted, move ipv6 address to nic ip */
                    if (deleteIp4 && hasIp6) {
                        SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid())
                                .set(VmNicVO_.ip, ip6.getIp()).set(VmNicVO_.gateway, ip6.getGateway()).update();
                    }
                }
            }
        }

        return msgs;
    }

    protected void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        if (op == OP_REMOVE_INSTANCE_OFFERING) {
            if (VmGlobalConfig.UPDATE_INSTANCE_OFFERING_TO_NULL_WHEN_DELETING.value(Boolean.class)) {
                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        List<InstanceOfferingInventory> offerings = action.getParentIssuerContext();
                        List<String> offeringUuids = transformAndRemoveNull(offerings, InstanceOfferingInventory::getUuid);

                        String sql = "update VmInstanceVO vm" +
                                " set vm.instanceOfferingUuid = null" +
                                " where vm.instanceOfferingUuid in (:offeringUuids)";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("offeringUuids", offeringUuids);
                        q.executeUpdate();
                    }
                }.run();
            }

            completion.success();
            return;
        }

        final List<VmDeletionStruct> vminvs = vmFromDeleteAction(action);
        if (vminvs == null || vminvs.isEmpty()) {
            completion.success();
            return;
        }

        if (op == OP_STOP) {
            List<StopVmInstanceMsg> msgs = new ArrayList<>();
            Set<String> vmStateCanStop = AbstractVmInstance.getAllowedStatesForOperation(StopVmInstanceMsg.class);

            boolean ignoreResourceReleaseFailure = Arrays.asList(
                    HostVO.class.getSimpleName(), PrimaryStorageVO.class.getSimpleName()
            ).contains(action.getParentIssuer());
            for (VmDeletionStruct inv : vminvs) {
                if (!vmStateCanStop.contains(inv.getInventory().getState())) {
                    continue;
                }
                StopVmInstanceMsg msg = new StopVmInstanceMsg();
                msg.setVmInstanceUuid(inv.getInventory().getUuid());
                msg.setGcOnFailure(true);
                msg.setIgnoreResourceReleaseFailure(ignoreResourceReleaseFailure);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                msgs.add(msg);
            }

            if (msgs.isEmpty()) {
                completion.success();
                return;
            }

            bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                @Override
                public void run(List<MessageReply> replies) {
                    if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                completion.fail(r.getError());
                                return;
                            }
                        }
                    }

                    completion.success();
                }
            });
        } else if (op == OP_DELETION) {
            boolean ignoreResourceReleaseFailure = Arrays.asList(
                    HostVO.class.getSimpleName(), PrimaryStorageVO.class.getSimpleName()
            ).contains(action.getParentIssuer());


            int parallelism = 10;
            Set<String> successfullyDeletedVmUuids = ConcurrentHashMap.newKeySet();
            List<VmDeletionStruct> deletionTargets = appendTemplatedCacheVmDeletionStructs(vminvs);
            Set<String> deletingVmUuids = collectVmUuids(deletionTargets);
            new While<>(deletionTargets).step((inv, noErrorCompletion) -> {
                VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer()) ||
                        ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                    msg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.KeepVolume.toString());
                }
                if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    if (inv.getDeletionPolicy() != null) {
                        msg.setDeletionPolicy(inv.getDeletionPolicy().toString());
                    }
                }

                if (VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString().equals(msg.getDeletionPolicy())) {
                    msg.setIgnoreResourceReleaseFailure(true);
                } else {
                    msg.setIgnoreResourceReleaseFailure(ignoreResourceReleaseFailure);
                }

                msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
                msg.setVmInstanceUuid(inv.getInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                bus.send(msg, new CloudBusCallBack(noErrorCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            successfullyDeletedVmUuids.add(msg.getVmInstanceUuid());
                        } else if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                            // TODO
                            logger.warn(reply.getError().toString());
                        }

                        noErrorCompletion.done();
                    }
                });
            }, parallelism).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer()) ||
                            PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
                        cleanupDeletedTemplatedVmInstanceRows(successfullyDeletedVmUuids, deletingVmUuids);
                    }

                    if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                        List<VmDeletionStruct> deletedVmStructs = deletionTargets.stream()
                                .filter(vm -> successfullyDeletedVmUuids.contains(vm.getInventory().getUuid()))
                                .collect(Collectors.toList());
                        dbf.removeByPrimaryKeys(deletedVmStructs.stream().map(vm -> vm.getInventory().getVmNics())
                                        .flatMap(List::stream).map(VmNicInventory::getUuid)
                                        .collect(Collectors.toList()),
                                VmNicVO.class);
                        List<String> cdRomUuids = deletedVmStructs.stream().map(vm -> vm.getInventory().getVmCdRoms())
                                .flatMap(List::stream).map(VmCdRomInventory::getUuid)
                                .collect(Collectors.toList());
                        dbf.removeByPrimaryKeys(cdRomUuids, VmCdRomVO.class);

                        List<String> vmUuidList = transform(deletedVmStructs, p -> p.getInventory().getUuid());
                        dbf.removeByPrimaryKeys(vmUuidList, VmInstanceVO.class);
                    }

                    completion.success();
                }
            });
        } else if (op == OP_DETACH_NIC) {
            final List<DetachNicFromVmMsg> msgs = new ArrayList<>();
            if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
                List<L3NetworkInventory> l3s = action.getParentIssuerContext();
                for (VmDeletionStruct vm : vminvs) {
                    for (L3NetworkInventory l3 : l3s) {
                        VmNicInventory nic = vm.getInventory().findNic(l3.getUuid());
                        if (nic == null) {
                            continue;
                        }

                        DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                        msg.setVmInstanceUuid(vm.getInventory().getUuid());
                        msg.setVmNicUuid(nic.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getInventory().getUuid());
                        msgs.add(msg);
                    }
                }
            } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
                List<IpRangeInventory> iprs = action.getParentIssuerContext();
                List<DetachNicFromVmMsg> dmsgs = handleDeletionForIpRange(vminvs, iprs);
                msgs.addAll(dmsgs);
            }

            detachVmNicCascade(msgs, true, completion);
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE || op == OP_STOP) {
            completion.success();
            return;
        }

        List<VmDeletionStruct> vminvs = vmFromDeleteAction(action);
        if (vminvs == null) {
            completion.success();
            return;
        }

        for (VmDeletionStruct inv : vminvs) {
            ErrorCode err = extEmitter.preDestroyVm(inv.getInventory());
            if (err != null) {
                completion.fail(err);
                return;
            }
        }

        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                HostVO.class.getSimpleName(),
                L3NetworkVO.class.getSimpleName(),
                IpRangeVO.class.getSimpleName(),
                PrimaryStorageVO.class.getSimpleName(),
                L2NetworkVO.class.getSimpleName(),
                InstanceOfferingVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<VmDeletionStruct> toVmDeletionStruct(Collection<VmInstanceVO> vos) {
        List<VmDeletionStruct> structs = new ArrayList<>();
        for (VmInstanceVO vo : vos) {
            VmDeletionStruct s = new VmDeletionStruct();
            s.setInventory(VmInstanceInventory.valueOf(vo));
            s.setDeletionPolicy(deletionPolicyManager.getDeletionPolicy(vo.getUuid()));
            structs.add(s);
        }
        return structs;
    }

    private List<VmDeletionStruct> appendTemplatedCacheVmDeletionStructs(List<VmDeletionStruct> structs) {
        Set<String> vmUuids = collectVmUuids(structs);
        if (vmUuids.isEmpty()) {
            return structs;
        }

        List<String> cacheVmUuids = Q.New(TemplatedVmInstanceCacheVO.class)
                .select(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid)
                .in(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, vmUuids)
                .listValues();
        cacheVmUuids.removeAll(vmUuids);
        if (cacheVmUuids.isEmpty()) {
            return structs;
        }

        List<VmInstanceVO> cacheVms = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, cacheVmUuids)
                .list();
        if (cacheVms.isEmpty()) {
            return structs;
        }

        List<VmDeletionStruct> ret = new ArrayList<>(structs);
        ret.addAll(toVmDeletionStruct(cacheVms));
        return ret;
    }

    private Set<String> collectVmUuids(Collection<VmDeletionStruct> structs) {
        return transformAndRemoveNull(structs, s -> s.getInventory().getUuid()).stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected List<VmDeletionStruct> vmFromDeleteAction(CascadeAction action) {
        List<VmDeletionStruct> ret = null;
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> huuids = transformAndRemoveNull(hosts, HostInventory::getUuid);

            Map<String, VmInstanceVO> vmvos = new HashMap<>();
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.add(VmInstanceVO_.hostUuid, SimpleQuery.Op.IN, huuids);
            q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
            List<VmInstanceVO> lst = q.list();
            for (VmInstanceVO vo : lst) {
                vmvos.put(vo.getUuid(), vo);
            }

            if (ClusterVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ClusterInventory> clusters = action.getRootIssuerContext();
                List<String> clusterUuids = transformAndRemoveNull(clusters, ClusterInventory::getUuid);
                q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.clusterUuid, Op.IN, clusterUuids);
                q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
                lst = q.list();
                for (VmInstanceVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            } else if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ZoneInventory> zones = action.getRootIssuerContext();
                List<String> zoneUuids = transformAndRemoveNull(zones, ZoneInventory::getUuid);
                q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.zoneUuid, Op.IN, zoneUuids);
                q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
                lst = q.list();
                for (VmInstanceVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            }

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos.values());
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> pruuids = transformAndRemoveNull(action.getParentIssuerContext(), PrimaryStorageInventory::getUuid);

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, VolumeVO vol, PrimaryStorageVO pr" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = vol.vmInstanceUuid" +
                            " and vol.primaryStorageUuid = pr.uuid" +
                            " and vol.type = :volType" +
                            " and pr.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("uuids", pruuids);
                    q.setParameter("volType", VolumeType.Root);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> l3uuids = transformAndRemoveNull(action.getParentIssuerContext(), L3NetworkInventory::getUuid);

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, L3NetworkVO l3, VmNicVO nic" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = nic.vmInstanceUuid" +
                            " and vm.state in (:vmStates)" +
                            " and nic.l3NetworkUuid = l3.uuid" +
                            " and l3.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("vmStates", Arrays.asList(
                            VmInstanceState.Stopped, VmInstanceState.Paused,
                            VmInstanceState.Running, VmInstanceState.Destroyed));
                    q.setParameter("uuids", l3uuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<IpRangeInventory> ipRanges = action.getParentIssuerContext();
            if (ipRanges.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> ipruuids = Q.New(NormalIpRangeVO.class)
                    .select(NormalIpRangeVO_.uuid)
                    .in(NormalIpRangeVO_.uuid, transform(ipRanges, IpRangeInventory::getUuid))
                    .listValues();

            if (ipruuids.isEmpty()) {
                return new ArrayList<>();
            }

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip, NormalIpRangeVO ipr" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = nic.vmInstanceUuid" +
                            " and vm.state in (:vmStates)" +
                            " and nic.uuid = ip.vmNicUuid" +
                            " and ip.ipRangeUuid = ipr.uuid" +
                            " and ipr.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("vmStates", Arrays.asList(
                            VmInstanceState.Stopped, VmInstanceState.Paused,
                            VmInstanceState.Running, VmInstanceState.Destroyed));
                    q.setParameter("uuids", ipruuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = transformAndRemoveNull(action.getParentIssuerContext(), AccountInventory::getUuid);
            
            List<VmInstanceVO> vmvos = ResourceHelper.findOwnResources(VmInstanceVO.class, auuids);
            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            int op = toDeletionOpCode(action);
            if (op == OP_NOPE || op == OP_STOP || op == OP_REMOVE_INSTANCE_OFFERING || op == OP_DETACH_NIC) {
                return null;
            }

            List<VmDeletionStruct> vms = vmFromDeleteAction(action);
            if (vms == null) {
                return null;
            }

            return action.copy().setParentIssuer(NAME).setParentIssuerContext(vms);
        }

        return null;
    }
}
