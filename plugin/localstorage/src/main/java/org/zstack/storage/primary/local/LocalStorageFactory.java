package org.zstack.storage.primary.local;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.MigrateNetworkExtensionPoint;
import org.zstack.compute.vm.*;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.trash.CreateRecycleExtensionPoint;
import org.zstack.core.trash.DeleteRecycleExtensionPoint;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.core.*;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.trash.InstallPathRecycleVO;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.storage.snapshot.PostMarkRootVolumeAsSnapshotExtension;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageFactory implements PrimaryStorageFactory, Component,
        MarshalVmOperationFlowExtensionPoint, HostDeleteExtensionPoint, VmAttachVolumeExtensionPoint,
        GetAttachableVolumeExtensionPoint, HostMaintenancePolicyExtensionPoint, AddExpandedQueryExtensionPoint, VolumeGetAttachableVmExtensionPoint,
        RecoverDataVolumeExtensionPoint, RecoverVmExtensionPoint, VmPreMigrationExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint,
        HostAfterConnectedExtensionPoint, InstantiateDataVolumeOnCreationExtensionPoint, PrimaryStorageAttachExtensionPoint,
        PostMarkRootVolumeAsSnapshotExtension, VolumeSnapshotCreationExtensionPoint, VmCapabilitiesExtensionPoint, PrimaryStorageDetachExtensionPoint,
        CreateRecycleExtensionPoint, AfterInstantiateVolumeExtensionPoint, CreateDataVolumeExtensionPoint, DeleteRecycleExtensionPoint {
    private final static CLogger logger = Utils.getLogger(LocalStorageFactory.class);
    public static PrimaryStorageType type = new PrimaryStorageType(LocalStorageConstants.LOCAL_STORAGE_TYPE) {
        @Override
        public boolean isSupportVmLiveMigration() {
            return supportVmLiveMigration &
                    LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.value(Boolean.class);
        }
    };

    static {
        type.setSupportVmLiveMigration(true);
        type.setSupportVolumeMigration(true);
        type.setSupportVolumeMigrationInCurrentPrimaryStorage(true);
        type.setOrder(999);
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private Map<String, LocalStorageBackupStorageMediator> backupStorageMediatorMap = new HashMap<String, LocalStorageBackupStorageMediator>();

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        class Context {
            String temporaryInstallPath;
            String hostUuid;
        }

        final Context ctx = new Context();

        template.setCreateTemporaryTemplate(new Flow() {
            String __name__ = "create-temporary-template";

            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                CreateTemporaryVolumeFromSnapshotMsg msg = new CreateTemporaryVolumeFromSnapshotMsg();
                msg.setSnapshot(paramIn.getSnapshot());
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setImageUuid(paramIn.getImage().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            ParamOut out = (ParamOut) data.get(ParamOut.class);
                            CreateTemporaryVolumeFromSnapshotReply r = reply.castReply();
                            out.setActualSize(r.getActualSize());
                            out.setSize(r.getSize());
                            ctx.temporaryInstallPath = r.getInstallPath();
                            ctx.hostUuid = r.getHostUuid();
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.temporaryInstallPath != null) {
                    LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setHostUuid(ctx.hostUuid);
                    msg.setPath(ctx.temporaryInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                ask.setImageMediaType(paramIn.getImage().getMediaType());
                ask.setImageUuid(paramIn.getImage().getUuid());
                bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                MessageReply areply = bus.call(ask);
                if (!areply.isSuccess()) {
                    trigger.fail(areply.getError());
                    return;
                }

                String bsInstallPath = ((BackupStorageAskInstallPathReply) areply).getInstallPath();
                UploadBitsFromLocalStorageToBackupStorageMsg msg = new UploadBitsFromLocalStorageToBackupStorageMsg();
                msg.setHostUuid(ctx.hostUuid);
                msg.setPrimaryStorageInstallPath(ctx.temporaryInstallPath);
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                msg.setBackupStorageInstallPath(bsInstallPath);
                msg.setImageUuid(paramIn.getImage().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());

                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        ParamOut out = (ParamOut) data.get(ParamOut.class);

                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            UploadBitsFromLocalStorageToBackupStorageReply r = reply.castReply();
                            out.setBackupStorageInstallPath(r.getBackupStorageInstallPath());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NoRollbackFlow() {
            String __name__ = "delete-temporary-volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                LocalStorageDirectlyDeleteBitsMsg msg = new LocalStorageDirectlyDeleteBitsMsg();
                msg.setHostUuid(ctx.hostUuid);
                msg.setPath(ctx.temporaryInstallPath);
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg);
                trigger.next();
            }
        });

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return type.toString();
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public void afterInstantiateVolume(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            return;
        }

        String psType = dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class).getType();
        if (!type.toString().equals(psType)) {
            return;
        }

        boolean hasBackingFile = false;
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg imsg = (InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg;
            ImageInventory image = imsg.getTemplateSpec().getInventory();
            if (ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
                hasBackingFile = true;
            }
        }
        
        VolumeInventory volume = msg.getVolume();
        volume.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        for (CreateQcow2VolumeProvisioningStrategyExtensionPoint exp : pluginRgty.getExtensionList(CreateQcow2VolumeProvisioningStrategyExtensionPoint.class)) {
            exp.saveQcow2VolumeProvisioningStrategy(volume, hasBackingFile);
        }
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        vo.setMountPath(msg.getUrl());
        vo = dbf.persistAndRefresh(vo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new LocalStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }

    @Override
    public void validateStorageProtocol(String protocol) {

    }

    private String makeMediatorKey(String hvType, String bsType) {
        return hvType + "-" + bsType;
    }

    public LocalStorageBackupStorageMediator getBackupStorageMediator(String hvType, String bsType) {
        LocalStorageBackupStorageMediator m = backupStorageMediatorMap.get(makeMediatorKey(hvType, bsType));
        if (m == null) {
            throw new OperationFailureException(operr("no LocalStorageBackupStorageMediator supporting hypervisor[%s] and backup storage type[%s] ",
                    hvType, bsType));
        }

        return m;
    }

    @Override
    public boolean start() {
        for (LocalStorageBackupStorageMediator m : pluginRgty.getExtensionList(LocalStorageBackupStorageMediator.class)) {
            for (String hvType : m.getSupportedHypervisorTypes()) {
                String key = makeMediatorKey(hvType, m.getSupportedBackupStorageType());
                LocalStorageBackupStorageMediator old = backupStorageMediatorMap.get(key);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate LocalStorageBackupStorageMediator[%s, %s]" +
                                    " for hypervisor type[%s] and backup storage type[%s]",
                            m, old, hvType, m.getSupportedBackupStorageType()));
                }

                backupStorageMediatorMap.put(key, m);
            }
        }

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                ResizeVolumeOnHypervisorReply rmsg = (ResizeVolumeOnHypervisorReply) msg;

                if (rmsg.getError() != null || rmsg.getVolume() == null) {
                    return;
                }

                VolumeInventory volume = rmsg.getVolume();

                String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                        .eq(PrimaryStorageVO_.uuid, volume.getPrimaryStorageUuid())
                        .findValue();
                final boolean isLocalPS = LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(psType);

                if (isLocalPS) {
                    String hostUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid)
                            .eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid()).findValue();
                    Long size = volume.getSize();

                    SQL.New(LocalStorageResourceRefVO.class)
                            .condAnd(LocalStorageResourceRefVO_.resourceUuid, SimpleQuery.Op.EQ, volume.getUuid())
                            .condAnd(LocalStorageResourceRefVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid)
                            .set(LocalStorageResourceRefVO_.size, size).update();
                }
            }
        }, ResizeVolumeOnHypervisorReply.class);

        VolumeSnapshotReferenceUtils.setGetResourceLocateHostUuidGetter(resourceUuid -> Q.New(LocalStorageResourceRefVO.class)
                .select(LocalStorageResourceRefVO_.hostUuid)
                .eq(LocalStorageResourceRefVO_.resourceUuid, resourceUuid)
                .findValue());
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Transactional(readOnly = true)
    private List<String> getAvailableLocalStorageInCluster(String clusterUuid) {
        String sql = "select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype" +
                " and pri.state = :state" +
                " and pri.status = :status";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        q.setParameter("state", PrimaryStorageState.Enabled);
        q.setParameter("status", PrimaryStorageStatus.Connected);
        return q.getResultList();
    }

    private boolean isRootVolumeOnLocalStorage(String rootVolumeUuid) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, rootVolumeUuid);
        return q.isExists();
    }

    @Override
    public Flow marshalVmOperationFlow(String previousFlowName, String nextFlowName, FlowChain chain, VmInstanceSpec spec) {
        if (VmAllocateHostFlow.class.getName().equals(nextFlowName) || VmAllocateHostAndPrimaryStorageFlow.class.getName().equals(nextFlowName)) {
            if (spec.getCurrentVmOperation() != VmOperation.NewCreate || !spec.getImageSpec().relyOnImageCache()) {
                return null;
            }

            String imageUuid = spec.getImageSpec().getInventory().getUuid();
            String requirdHostUuid = spec.getRequiredHostUuid();
            List<String> cacheUrls = Q.New(ImageCacheVO.class).select(ImageCacheVO_.installUrl)
                    .eq(ImageCacheVO_.imageUuid, imageUuid)
                    .listValues();

            if (!cacheUrls.stream().allMatch(it -> it.contains("hostUuid://"))) {
                return null;
            }

            List<String> cachedHostUuids = cacheUrls.stream().map(it -> LocalStorageUtils.getHostUuidFromInstallUrl(it)).collect(Collectors.toList());
            if (requirdHostUuid != null && !cachedHostUuids.contains(requirdHostUuid)) {
                return new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.fail(operr("creation rely on image cache[uuid:%s, locate host uuids: [%s]], cannot create other places.", imageUuid, cachedHostUuids));

                    }
                };
            } else if (requirdHostUuid == null) {
                spec.setRequiredHostUuid(cachedHostUuids.get(0));
            }
        }

        if (VmAllocatePrimaryStorageFlow.class.getName().equals(nextFlowName)) {
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                List<String> localStorageUuids = getAvailableLocalStorageInCluster(spec.getDestHost().getClusterUuid());
                if (isEmpty(localStorageUuids)) {
                    return null;
                }

                String rootPs = spec.getRootDisk().getPrimaryStorageUuid();
                List<String> rootPsCandidates = spec.getCandidatePrimaryStorageUuidsForRootVolume();
                if (rootPs == null && !isEmpty(rootPsCandidates) && rootPsCandidates.size() == 1) {
                    rootPs = rootPsCandidates.get(0);
                }

                boolean requireNoneLocalStorage = rootPs != null && Q.New(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.uuid, rootPs)
                        .notEq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                        .isExists();
                requireNoneLocalStorage = requireNoneLocalStorage && (isEmpty(spec.getNonTemplateDeprecatedDisksSpecs()) ||
                        spec.getRequiredPrimaryStorageUuidForDataVolume() != null && Q.New(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.uuid, spec.getRequiredPrimaryStorageUuidForDataVolume())
                        .notEq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                        .isExists());
                if (requireNoneLocalStorage) {
                    return null;
                }

                boolean isOnlyLocalStorage = SQL.New("select pri.uuid" +
                                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                                " where pri.uuid = ref.primaryStorageUuid" +
                                " and ref.clusterUuid = :cuuid" +
                                " and pri.type != :ptype", String.class)
                        .param("cuuid", spec.getDestHost().getClusterUuid())
                        .param("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                        .list().isEmpty();

                if (!isOnlyLocalStorage && (rootPs != null || spec.getRequiredPrimaryStorageUuidForDataVolume() != null)) {
                    return new LocalStorageDesignatedAllocateCapacityFlow();
                } else {
                    return new LocalStorageDefaultAllocateCapacityFlow();
                }
            }
        } else if (spec.getCurrentVmOperation() == VmOperation.AttachVolume) {
            VolumeInventory volume = spec.getDestDataVolumes().get(0);
            if (VolumeStatus.NotInstantiated.toString().equals(volume.getStatus())
                    && VmAllocatePrimaryStorageForAttachingDiskFlow.class.getName().equals(nextFlowName)) {
                if (isRootVolumeOnLocalStorage(spec.getVmInventory().getRootVolumeUuid())) {
                    return new LocalStorageAllocateCapacityForAttachingVolumeFlow();
                }
            }
        } else if (spec.getCurrentVmOperation() == VmOperation.Migrate && isRootVolumeOnLocalStorage(spec.getVmInventory().getRootVolumeUuid())
                && VmMigrateOnHypervisorFlow.class.getName().equals(nextFlowName)) {
            if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(spec.getVmInventory().getHypervisorType())) {
                return new LocalStorageKvmMigrateVmFlow();
            } else {
                throw new OperationFailureException(operr("local storage doesn't support live migration for hypervisor[%s]", spec.getVmInventory().getHypervisorType()));
            }
        }

        return null;
    }

    @Override
    public void preDeleteHost(HostInventory inventory) {
    }

    @Override
    public void beforeDeleteHost(final HostInventory inventory) {
        // TODO re-write and add notifications to affected resoruces
        // TODO move the logic to cascade extension

        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.select(LocalStorageHostRefVO_.primaryStorageUuid);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, inventory.getUuid());
        List<String> psUuids = q.listValue();
        if(psUuids == null){
            psUuids = new ArrayList<>();
        }

        // maybe localStorage is detached
        List<String> psListForLocalStorageResource = Q.New(LocalStorageResourceRefVO.class)
                .select(LocalStorageResourceRefVO_.primaryStorageUuid)
                .eq(LocalStorageResourceRefVO_.hostUuid, inventory.getUuid())
                .listValues();
        if(psListForLocalStorageResource == null){
            psListForLocalStorageResource = new ArrayList<>();
        }

        if (psUuids.isEmpty() && psListForLocalStorageResource.isEmpty()){
            return;
        }

        // merge and duplicate
        psUuids.addAll(psListForLocalStorageResource);
        Set<String> psUuidSet = new HashSet<>(psUuids);
        psUuids.clear();
        psUuids.addAll(psUuidSet);

        logger.debug(String.format("the host[uuid:%s] belongs to the local storage[uuid:%s], starts to delete vms and" +
                " volumes on the host", inventory.getUuid(), String.join(",", psUuids)));

        List<String> finalPsUuids = psUuids;
        final List<String> vmUuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select vm.uuid" +
                        " from VolumeVO vol, LocalStorageResourceRefVO ref, VmInstanceVO vm" +
                        " where ref.primaryStorageUuid in :psUuids" +
                        " and vol.type = :vtype" +
                        " and ref.resourceUuid = vol.uuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid" +
                        " and vm.uuid = vol.vmInstanceUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("vtype", VolumeType.Root);
                q.setParameter("rtype", VolumeVO.class.getSimpleName());
                q.setParameter("huuid", inventory.getUuid());
                q.setParameter("psUuids", finalPsUuids);
                return q.getResultList();
            }
        }.call();

        // destroy vms
        if (!vmUuids.isEmpty()) {
            List<DestroyVmInstanceMsg> msgs = transformAndRemoveNull(vmUuids, uuid -> {
                DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                msg.setVmInstanceUuid(uuid);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                return msg;
            });

            final FutureCompletion completion = new FutureCompletion(null);
            bus.send(msgs, new CloudBusListCallBack(completion) {
                @Override
                public void run(List<MessageReply> replies) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            String vmUuid = vmUuids.get(replies.indexOf(r));
                            logger.warn(String.format("failed to destroy the vm[uuid:%s], %s", vmUuid, r.getError()));
                        }
                    }

                    completion.success();
                }
            });

            completion.await(TimeUnit.MINUTES.toMillis(15));
        }

        final List<String> volUuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select vol.uuid" +
                        " from VolumeVO vol, LocalStorageResourceRefVO ref" +
                        " where ref.primaryStorageUuid in :psUuids" +
                        " and vol.type = :vtype" +
                        " and ref.resourceUuid = vol.uuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuids", finalPsUuids);
                q.setParameter("vtype", VolumeType.Data);
                q.setParameter("rtype", VolumeVO.class.getSimpleName());
                q.setParameter("huuid", inventory.getUuid());
                return q.getResultList();
            }
        }.call();

        // delete data volumes
        if (!volUuids.isEmpty()) {
            List<DeleteVolumeMsg> msgs = transformAndRemoveNull(volUuids, uuid -> {
                DeleteVolumeMsg msg = new DeleteVolumeMsg();
                msg.setUuid(uuid);
                msg.setDetachBeforeDeleting(true);
                msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.DBOnly.toString());
                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, uuid);
                return msg;
            });

            final FutureCompletion completion = new FutureCompletion(null);
            bus.send(msgs, new CloudBusListCallBack(completion) {
                @Override
                public void run(List<MessageReply> replies) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            String uuid = volUuids.get(replies.indexOf(r));
                            //TODO
                            logger.warn(String.format("failed to delete the data volume[uuid:%s], %s", uuid,
                                    r.getError()));
                        }
                    }

                    completion.success();
                }
            });

            completion.await(TimeUnit.MINUTES.toMillis(15));
        }

        // decrease ps capacity
        for (String priUuid : psUuids) {
            RemoveHostFromLocalStorageMsg msg = new RemoveHostFromLocalStorageMsg();
            msg.setPrimaryStorageUuid(priUuid);
            msg.setHostUuid(inventory.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                //TODO
                logger.warn(String.format("failed to remove host[uuid:%s] from local primary storage[uuid:%s], %s",
                        inventory.getUuid(), priUuid, reply.getError()));
            } else {
                logger.debug(String.format("removed host[uuid:%s] from local primary storage[uuid:%s]",
                        inventory.getUuid(), priUuid));
            }
        }
    }

    @Override
    public void afterDeleteHost(final HostInventory inventory) {
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, final VolumeInventory volume) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, list(vm.getRootVolumeUuid(), volume.getUuid()));
        q.groupBy(LocalStorageResourceRefVO_.hostUuid);
        long count = q.count();


        if (count == 0) {
            return;
        }

        // if count is 1, multi primary storage is indicated
        if (count == 1) {
            if (!Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, volume.getUuid()).isExists()) {
                return;
            }

            String vmClusterUuid = vm.getClusterUuid();
            String volumeHostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, volume.getUuid()).findValue();

            if (!Q.New(HostVO.class)
                    .eq(HostVO_.uuid, volumeHostUuid)
                    .eq(HostVO_.clusterUuid, vmClusterUuid).isExists()) {
                throw new OperationFailureException(operr("Can't attach volume to VM, no qualified cluster"));
            }
        }

        if (count == 2) {
            q = dbf.createQuery(LocalStorageResourceRefVO.class);
            q.select(LocalStorageResourceRefVO_.hostUuid);
            q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, vm.getRootVolumeUuid());
            String rootHost = q.findValue();

            q = dbf.createQuery(LocalStorageResourceRefVO.class);
            q.select(LocalStorageResourceRefVO_.hostUuid);
            q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, volume.getUuid());
            String dataHost = q.findValue();

            if (!rootHost.equals(dataHost)) {
                throw new OperationFailureException(operr("cannot attach the data volume[uuid:%s] to the vm[uuid:%s]." +
                                " Both vm's root volume and the data volume are" +
                                " on local primary storage, but they are on different hosts." +
                                " The root volume[uuid:%s] is on the host[uuid:%s] but the data volume[uuid: %s]" +
                                " is on the host[uuid: %s]",
                        volume.getUuid(), vm.getUuid(), vm.getRootVolumeUuid(),
                        rootHost, volume.getUuid(), dataHost));
            }
        }
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {

    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {

    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumeVO> returnAttachableVolumes(VmInstanceInventory vm, List<VolumeVO> candidates) {
        // find instantiated volumes
        List<String> volUuids = transformAndRemoveNull(candidates,
                arg -> VolumeStatus.Ready == arg.getStatus() ? arg.getUuid() : null);

        if (volUuids.isEmpty()) {
            return candidates;
        }

        List<String> vmAllVolumeUuids = transformAndRemoveNull(vm.getAllVolumes(), VolumeInventory::getUuid);

        // root volume could be located at a shared storage
        String sql = "select ref.hostUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid in (:volUuids)" +
                " and ref.resourceType = :rtype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("volUuids", vmAllVolumeUuids);
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        List<String> ret = q.getResultList();

        String hostUuid = vm.getHostUuid();
        if (!ret.isEmpty()) {
            hostUuid = ret.get(0);
        }

        sql = "select ref.resourceUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid in (:uuids)" +
                " and ref.resourceType = :rtype" +
                " and ref.hostUuid != :huuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", volUuids);
        q.setParameter("huuid", hostUuid);
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        final List<String> toExclude = q.getResultList();

        candidates = transformAndRemoveNull(candidates, arg -> toExclude.contains(arg.getUuid()) ? null : arg);

        return candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, HostMaintenancePolicy> getHostMaintenanceVmOperationPolicy(HostInventory host) {
        Map<String, HostMaintenancePolicy> result = new HashMap<>();
        String sql = "select vm.uuid" +
                " from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref, VmInstanceVO vm, VolumeVO vol" +
                " where ps.uuid = ref.primaryStorageUuid" +
                " and ps.type = :type" +
                " and ref.clusterUuid = :cuuid" +
                " and vol.primaryStorageUuid = ps.uuid" +
                " and vm.rootVolumeUuid = vol.uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("type", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        q.setParameter("cuuid", host.getClusterUuid());
        List<String> vmUuids = q.getResultList();
        vmUuids.forEach(it -> result.put(it, HostMaintenancePolicy.StopVm));
        return result;
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<>();

        ExpandedQueryStruct s = new ExpandedQueryStruct();
        s.setExpandedField("localStorageHostRef");
        s.setExpandedInventoryKey("resourceUuid");
        s.setForeignKey("uuid");
        s.setInventoryClass(LocalStorageResourceRefInventory.class);
        s.setInventoryClassToExpand(VolumeInventory.class);
        structs.add(s);

        s = new ExpandedQueryStruct();
        s.setExpandedField("localStorageHostRef");
        s.setExpandedInventoryKey("resourceUuid");
        s.setForeignKey("uuid");
        s.setInventoryClass(LocalStorageResourceRefInventory.class);
        s.setInventoryClassToExpand(VolumeSnapshotInventory.class);
        structs.add(s);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VmInstanceVO> returnAttachableVms(VolumeInventory vol, List<VmInstanceVO> candidates) {
        String sql = "select ref.hostUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid = :uuid" +
                " and ref.resourceType = :rtype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", vol.getUuid());
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            return candidates;
        }

        List<VmInstanceVO> candidatesCopy = Lists.newArrayList(candidates);
        for (VmInstanceVO vo : candidates) {
            PrimaryStorageVO psVo = dbf.findByUuid(vo.getRootVolume().getPrimaryStorageUuid(), PrimaryStorageVO.class);
            if (LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(psVo.getType()) && VolumeStatus.NotInstantiated.toString().equals(vol.getStatus())) {
                String volumeUuid = vo.getRootVolumeUuid();
                VolumeVO rootVolumeVO = dbf.findByUuid(volumeUuid, VolumeVO.class);
                boolean avaliableHost = Q.New(LocalStorageHostRefVO.class)
                        .gte(LocalStorageHostRefVO_.availableCapacity, vol.getSize())
                        .eq(LocalStorageHostRefVO_.primaryStorageUuid, rootVolumeVO.getPrimaryStorageUuid())
                        .isExists();
                if (!avaliableHost) {
                    candidatesCopy.remove(vo);
                }
            }
        }

        if (candidatesCopy.isEmpty()){
            return candidatesCopy;
        }

        String hostUuid = ret.get(0);

        List<String> vmRootVolumeUuids = transformAndRemoveNull(candidatesCopy, VmInstanceVO::getRootVolumeUuid);

        // exclude: vm root volume is local and root volume is not on target host
        sql = "select ref.resourceUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.hostUuid != :huuid" +
                " and ref.resourceUuid in (:rootVolumeUuids)" +
                " and ref.resourceType = :rtype";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("rootVolumeUuids", vmRootVolumeUuids);
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        final List<String> toExclude = q.getResultList();

        candidatesCopy = transformAndRemoveNull(candidatesCopy,
                arg -> toExclude.contains(arg.getRootVolumeUuid()) ? null : arg);

        // exclude: vm hostUuid not equals target volume hostUuid
        candidatesCopy = transformAndRemoveNull(candidatesCopy,
                arg -> arg.getHostUuid() != null && !hostUuid.equals(arg.getHostUuid()) ? null : arg);

        return candidatesCopy;
    }

    @Override
    public void preRecoverDataVolume(VolumeInventory vol) {
        if (vol.getPrimaryStorageUuid() == null) {
            return;
        }

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(PrimaryStorageVO_.type);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, vol.getPrimaryStorageUuid());
        String type = q.findValue();
        if (!LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(type)) {
            return;
        }

        SimpleQuery<LocalStorageResourceRefVO> rq = dbf.createQuery(LocalStorageResourceRefVO.class);
        rq.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, vol.getUuid());
        rq.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        if (!rq.isExists()) {
            throw new OperationFailureException(operr("the data volume[name:%s, uuid:%s] is on the local storage[uuid:%s]; however," +
                                    "the host on which the data volume is has been deleted. Unable to recover this volume",
                            vol.getName(), vol.getUuid(), vol.getPrimaryStorageUuid()));
        }
    }

    @Override
    public void beforeRecoverDataVolume(VolumeInventory vol) {
    }

    @Override
    public void afterRecoverDataVolume(VolumeInventory vol) {

    }

    @Override
    @Transactional(readOnly = true)
    public void preRecoverVm(VmInstanceInventory vm) {
        String rootVolUuid = vm.getRootVolumeUuid();

        String sql = "select ps.uuid" +
                " from PrimaryStorageVO ps, VolumeVO vol" +
                " where ps.uuid = vol.primaryStorageUuid" +
                " and vol.uuid = :uuid" +
                " and ps.type = :pstype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", rootVolUuid);
        q.setParameter("pstype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        String psuuid = dbf.find(q);
        if (psuuid == null) {
            return;
        }

        sql = "select count(ref)" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid = :uuid" +
                " and ref.resourceType = :rtype";
        TypedQuery<Long> rq = dbf.getEntityManager().createQuery(sql, Long.class);
        rq.setParameter("uuid", rootVolUuid);
        rq.setParameter("rtype", VolumeVO.class.getSimpleName());
        long count = rq.getSingleResult();
        if (count == 0) {
            throw new OperationFailureException(operr("unable to recover the vm[uuid:%s, name:%s]. The vm's root volume is on the local" +
                                    " storage[uuid:%s]; however, the host on which the root volume is has been deleted",
                            vm.getUuid(), vm.getName(), psuuid));
        }
    }

    @Override
    public void beforeRecoverVm(VmInstanceInventory vm) {

    }

    @Override
    public void afterRecoverVm(VmInstanceInventory vm) {

    }

    @Transactional(readOnly = true)
    private ErrorCode checkVmMigrationCapability(VmInstanceInventory vm) {
        // if not local storage, return
        String sql = "select count(ps)" +
                " from PrimaryStorageVO ps" +
                " where ps.uuid = :uuid" +
                " and ps.type = :type";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("uuid", vm.getRootVolume().getPrimaryStorageUuid());
        q.setParameter("type", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        q.setMaxResults(1);
        Long count = q.getSingleResult();
        if (count == 0) {
            return null;
        }

        // forbid live migration with data volumes for local storage
        if (vm.getAllDiskVolumes().size() > 1) {
            return operr("unable to live migrate vm[uuid:%s] with data volumes on local storage." +
                    " Need detach all data volumes first.", vm.getUuid());
        }

        if (!ImagePlatform.Linux.toString().equals(vm.getPlatform())) {
            return operr("unable to live migrate vm[uuid:%s] with local storage." +
                    " Only linux guest is supported. Current platform is [%s]", vm.getUuid(), vm.getPlatform());
        }

        if (IsoOperator.isIsoAttachedToVm(vm.getUuid())) {
            return operr("unable to live migrate vm[uuid:%s] with ISO on local storage." +
                    " Need detach all ISO first.", vm.getUuid());
        }

        return null;
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType vmMigrationType, String dstHostUuid, Completion completion) {
        if (!VmMigrationType.HostMigration.equals(vmMigrationType)) {
            completion.success();
            return;
        }

        ErrorCode err = checkVmMigrationCapability(vm);

        if (err != null) {
            completion.fail(err);
            return;
        }

        completion.success();
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (!Q.New(LocalStorageHostRefVO.class).eq(LocalStorageHostRefVO_.hostUuid, host.getUuid()).isExists()) {
            return;
        }

        recalculatePrimaryStorageCapacity(host.getClusterUuid(), false);
    }

    private void recalculatePrimaryStorageCapacity(String clusterUuid, boolean needRecalculateRef) {
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.add(PrimaryStorageClusterRefVO_.clusterUuid, Op.EQ, clusterUuid);
        List<PrimaryStorageClusterRefVO> refs = q.list();
        if (refs != null && !refs.isEmpty()) {
            for (PrimaryStorageClusterRefVO ref : refs) {
                LocalStorageRecalculateCapacityMsg msg = new LocalStorageRecalculateCapacityMsg();
                msg.setNeedRecalculateRef(needRecalculateRef);
                msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ref.getPrimaryStorageUuid());
                bus.send(msg);
            }
        }
    }

    private void recalculatePrimaryStorageCapacity(String clusterUuid) {
        recalculatePrimaryStorageCapacity(clusterUuid, true);
    }

    @Override
    public String getPrimaryStorageTypeForInstantiateDataVolumeOnCreationExtensionPoint() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public void instantiateDataVolumeOnCreation(InstantiateVolumeMsg msg, VolumeInventory volume, ReturnValueCompletion<VolumeInventory> completion) {
        String hostUuid = null;
        if (msg.getHostUuid() != null) {
            hostUuid = msg.getHostUuid();
        } else {
            if (msg.getSystemTags() != null) {
                for (String stag : msg.getSystemTags()) {
                    if (LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.isMatch(stag)) {
                        hostUuid = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.getTokenByTag(
                                stag,
                                LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN
                        );
                        break;
                    }
                }
            }

            if (hostUuid == null) {
                throw new OperationFailureException(argerr("To create data volume on the local primary storage, you must specify the host that" +
                                        " the data volume is going to be created using the system tag [%s]",
                                LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.getTagFormat()));
            }
        }

        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, hostUuid);
        q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
        if (!q.isExists()) {
            throw new OperationFailureException(argerr("the host[uuid:%s] doesn't belong to the local primary storage[uuid:%s]", hostUuid, msg.getPrimaryStorageUuid()));
        }

        InstantiateVolumeOnPrimaryStorageMsg imsg;
        if (msg instanceof InstantiateTemporaryRootVolumeMsg) {
            InstantiateTemporaryRootVolumeMsg tmsg = (InstantiateTemporaryRootVolumeMsg) msg;
            if (ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(tmsg.getTemplateSpec().getInventory().getMediaType())) {
                imsg = new InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg();
                ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(tmsg.getOriginVolumeUuid());
                ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setTemplateSpec(tmsg.getTemplateSpec());
            } else {
                imsg = new InstantiateTemporaryVolumeOnPrimaryStorageMsg();
                ((InstantiateTemporaryVolumeOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(tmsg.getOriginVolumeUuid());
            }
        } else if (msg instanceof InstantiateRootVolumeMsg) {
            InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg irmsg = new InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg();
            irmsg.setTemplateSpec(((InstantiateRootVolumeMsg) msg).getTemplateSpec());
            imsg = irmsg;
        } else if (msg instanceof InstantiateMemoryVolumeMsg) {
            imsg = new InstantiateMemoryVolumeOnPrimaryStorageMsg();
        } else {
            imsg = new InstantiateVolumeOnPrimaryStorageMsg();
        }

        imsg.setVolume(volume);
        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class)));
        // For root volume with backing file
        imsg.setVolumeLuksAgentSpec(msg.getVolumeLuksAgentSpec());
        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(imsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    List<AfterInstantiateVolumeExtensionPoint> exts = pluginRgty.getExtensionList(AfterInstantiateVolumeExtensionPoint.class);
                    for (AfterInstantiateVolumeExtensionPoint ext : exts) {
                        ext.afterInstantiateVolume(imsg);
                    }
                    completion.success(((InstantiateVolumeOnPrimaryStorageReply) reply).getVolume());
                }
            }
        });
    }

    private boolean isThereOtherNonLocalStoragePrimaryStorageForTheHost(String hostUuid, String localStorageUuid) {
        long count = SQL.New( "select count(pri)" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = host.clusterUuid" +
                " and host.uuid = :huuid" +
                " and pri.uuid != :puuid" +
                " and pri.type != :pstype", Long.class)
                .param("huuid", hostUuid)
                .param("puuid", localStorageUuid)
                .param("pstype", LocalStorageConstants.LOCAL_STORAGE_TYPE).find();
        return count > 0;
    }

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {

    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (!inventory.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
            return;
        }
        recalculatePrimaryStorageCapacity(clusterUuid);
        initilizedLocalStorageSystemTags(inventory, clusterUuid);
    }

    public void initilizedLocalStorageSystemTags(PrimaryStorageInventory inventory, String clusterUuid) {
        List<String> refHostUuids = Q.New(LocalStorageHostRefVO.class)
                .select(LocalStorageHostRefVO_.hostUuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, inventory.getUuid())
                .listValues();

        if (refHostUuids.isEmpty()) {
            return;
        }

        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.uuid, refHostUuids)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .listValues();

        hostUuids.forEach(hostUuid -> {
            if (!hostHasInitializedTag(hostUuid, inventory.getUuid())) {
                SystemTagCreator creator = LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.newSystemTagCreator(hostUuid);
                creator.inherent = true;
                creator.unique = false;
                creator.setTagByTokens(map(e(LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED_TOKEN, inventory.getUuid())));
                creator.create();
            }
        });
    }

    public boolean hostHasInitializedTag(String hostUuid, String psUuid) {
        List<Map<String, String>> tags = LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.getTokensOfTagsByResourceUuid(hostUuid);
        for (Map<String, String> tag : tags) {
            if (tag.get(LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED_TOKEN).equals(psUuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory snapshot) {

        new SQLBatch(){

            @Override
            protected void scripts() {
                String type = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, snapshot.getPrimaryStorageUuid()).select(PrimaryStorageVO_.type).findValue();
                if(!type.equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)){
                    return;
                }
                LocalStorageResourceRefVO ref = new LocalStorageResourceRefVO();
                ref.setPrimaryStorageUuid(snapshot.getPrimaryStorageUuid());
                ref.setSize(snapshot.getSize());
                ref.setResourceType(VolumeSnapshotVO.class.getSimpleName());
                ref.setResourceUuid(snapshot.getUuid());
                ref.setHostUuid(Q.New(LocalStorageResourceRefVO.class)
                        .select(LocalStorageResourceRefVO_.hostUuid)
                        .eq(LocalStorageResourceRefVO_.resourceUuid, snapshot.getVolumeUuid()).findValue());
                persist(ref);
            }
        }.execute();

    }

    protected String getDestMigrationAddress(String srcHostUuid, String dstHostUuid){
        MigrateNetworkExtensionPoint.MigrateInfo migrateIpInfo = null;
        for (MigrateNetworkExtensionPoint ext: pluginRgty.getExtensionList(MigrateNetworkExtensionPoint.class)) {
            MigrateNetworkExtensionPoint.MigrateInfo r = ext.getMigrationAddressForVM(srcHostUuid, dstHostUuid);
            if (r == null) {
                continue;
            }

            migrateIpInfo = r;
        }

        return migrateIpInfo != null ? migrateIpInfo.dstMigrationAddress :
                Q.New(HostVO.class).eq(HostVO_.uuid, dstHostUuid).select(HostVO_.managementIp).findValue();
    }

    private Boolean isLocalStorage(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, type.toString())
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .isExists();
    }

    @Override
    public void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities) {
        if (capabilities.isSupportLiveMigration()) {
            // this function will check if vm on local
            ErrorCode err = checkVmMigrationCapability(inv);

            if (err != null) {
                capabilities.setSupportLiveMigration(false);
            }
        }
    }

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (!inventory.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
            return;
        }
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.clusterUuid, clusterUuid).listValues();

        for (String hostUuid: hostUuids) {
            LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.deleteInherentTag(hostUuid,
                    LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.instantiateTag(map(
                    e(LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED_TOKEN, inventory.getUuid()))));
        }
    }

    @Override
    public void setHostUuid(InstallPathRecycleVO vo, String primaryStorageUuid) {
        PrimaryStorageVO ps = dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class);
        if (!ps.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
            return;
        }
        List<String> hostUuids = Q.New(LocalStorageResourceRefVO.class).
                eq(LocalStorageResourceRefVO_.primaryStorageUuid, primaryStorageUuid).
                eq(LocalStorageResourceRefVO_.resourceUuid, vo.getResourceUuid()).
                select(LocalStorageResourceRefVO_.hostUuid).listValues();
        if (!hostUuids.isEmpty()) {
            vo.setHostUuid(hostUuids.get(0));
        }
    }

    @Override
    public String makeSureInstallPathNotUsed(InstallPathRecycleInventory inv) {
        List<String> volumeUuids = Q.New(VolumeVO.class).eq(VolumeVO_.installPath, inv.getInstallPath()).select(VolumeVO_.uuid).listValues();
        if (volumeUuids.isEmpty()) {
            return null;
        }

        volumeUuids = volumeUuids.stream().filter(volumeUuid -> {
            String hostUuid = LocalStorageAllocatorFactory.getHostUuidFromURIScheme(String.format("volume://%s", volumeUuid));
            return Objects.equals(hostUuid, inv.getHostUuid());
        }).collect(Collectors.toList());

        return volumeUuids.isEmpty() ? null : String.format("%s is still in using by volume[uuids:%s], " +
                "cannot remove it from trash before delete them", inv.getInstallPath(), volumeUuids);
    }

    @Override
    public String buildAllocatedInstallUrl(InstallPathRecycleInventory inv) {
        LocalStorageUtils.InstallPath path = new LocalStorageUtils.InstallPath();
        path.installPath = inv.getInstallPath();
        path.hostUuid = inv.getHostUuid();
        return path.makeFullPath();
    }

    @Override
    public void preCreateVolume(VolumeCreateMessage msg) {
        String diskOffering = msg.getDiskOfferingUuid();
        if (diskOffering == null || !DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null || config.getAllocate().getPrimaryStorage() == null) {
            return;
        }

        msg.setPrimaryStorageUuid(config.getAllocate().getPrimaryStorage().getUuid());
        String psUuid = msg.getPrimaryStorageUuid();
        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid,psUuid)
                .findValue();
        if (!type.toString().equals(psType)) {
            return;
        }

        long total = Q.New(LocalStorageHostRefVO.class).eq(LocalStorageHostRefVO_.primaryStorageUuid, psUuid).count();
        List<LocalStorageHostRefVO> filterRefs = SQL.New("select ref from LocalStorageHostRefVO ref" +
                " where ref.primaryStorageUuid = :psUuid", LocalStorageHostRefVO.class)
                .param("psUuid", psUuid)
                .limit(100)
                .paginateCollectionUntil(total, (LocalStorageHostRefVO ref) -> PrimaryStorageCapacityChecker.New(psUuid,
                                ref.getAvailableCapacity(), ref.getTotalPhysicalCapacity(), ref.getAvailablePhysicalCapacity())
                                .checkRequiredSize(msg.getDiskSize()), 10);

        if (filterRefs.isEmpty()) {
            throw new OperationFailureException(err(HostAllocatorError.NO_AVAILABLE_HOST,
                    "the local primary storage[uuid:%s] has no hosts with enough disk capacity[%s bytes] required by the disk offering[uuid:%s]",
                    psUuid, msg.getDiskSize(), diskOffering
            ));
        }

        msg.addSystemTag(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                Collections.singletonMap(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN,
                        filterRefs.get(new Random().nextInt(filterRefs.size())).getHostUuid())
        ));
    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {
    }

    @Override
    public void afterCreateVolume(VolumeVO volume) {

    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
        if (treply != null && !treply.isSuccess()) {
            completion.fail(treply.getError());
            return;
        }

        List<LocalStorageResourceRefVO> refs = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
            if (!isLocalStorage(job.getPrimaryStorageUuid())) {
                continue;
            }

            LocalStorageResourceRefVO ref = new LocalStorageResourceRefVO();
            ref.setPrimaryStorageUuid(job.getPrimaryStorageUuid());
            ref.setResourceType(VolumeSnapshotVO.class.getSimpleName());
            VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getLockedVmInstanceUuids().get(0)).find();
            ref.setHostUuid(vmInstanceVO.getHostUuid() != null ? vmInstanceVO.getHostUuid() : vmInstanceVO.getLastHostUuid());
            ref.setCreateDate(job.getVolumeSnapshotStruct().getCurrent().getCreateDate());
            ref.setLastOpDate(job.getVolumeSnapshotStruct().getCurrent().getLastOpDate());
            ref.setResourceUuid(job.getVolumeSnapshotStruct().getCurrent().getUuid());
            ref.setSize(treply == null ? 0L : treply.getSnapshotsResults().stream()
                    .filter(r -> r.getVolumeUuid().equals(job.getVolumeUuid()))
                    .findFirst()
                    .map(TakeSnapshotsOnKvmResultStruct::getSize)
                    .orElse(0L));
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }

        completion.success();
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {

    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }
}
