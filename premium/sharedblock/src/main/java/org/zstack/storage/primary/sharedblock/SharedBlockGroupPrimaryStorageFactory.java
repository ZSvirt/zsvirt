package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storageDevice.GetScsiLunCandidatesExtensionPoint;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.*;
import org.zstack.storage.primary.ChangePrimaryStorageStatusMsg;
import org.zstack.storage.snapshot.PostMarkRootVolumeAsSnapshotExtension;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.NumberUtils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmBackend.*;

public class SharedBlockGroupPrimaryStorageFactory implements PrimaryStorageFactory,
        CreateTemplateFromVolumeSnapshotExtensionPoint, Component,
        HostDeleteExtensionPoint, PrimaryStorageDetachExtensionPoint, PostMarkRootVolumeAsSnapshotExtension,
        PrimaryStorageAttachExtensionPoint, KVMStartVmExtensionPoint, KVMAttachVolumeExtensionPoint,
        KVMDetachVolumeExtensionPoint, KVMMergeSnapshotExtensionPoint, KVMTakeSnapshotExtensionPoint,
        HostResizeVolumeExtensionPoint, KvmSetupSelfFencerExtensionPoint, GetScsiLunCandidatesExtensionPoint,
        KVMCheckSnapshotExtensionPoint, KVMBlockCommitExtensionPoint, KVMBlockPullExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SharedBlockGroupPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);

    static {
        type.setSupportHeartbeatFile(true);
        type.setOrder(699);
        type.setSupportVmLiveMigration(true);
        type.setSupportSharedVolume(true);
        type.setSupportConfigVolumeProvisionStrategy(true);
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    @Override
    public boolean start() {
        setupGlobalConfigValidateExtension();
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void setupGlobalConfigValidateExtension() {
        SharedBlockGlobalConfig.QCOW2_CLUSTER_SIZE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                Integer number = Integer.valueOf(newValue);
                if (!NumberUtils.isPowerOf2(number)) {
                    throw new OperationFailureException(argerr("the value[%s] is not power of 2", newValue));
                }
            }
        });
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {

        SharedBlockGroupVO svo = new SharedBlockGroupVO(vo);

        svo.setType(type.toString());
        svo.setMountPath(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
        svo.setUrl(String.format("sharedblock://%s", vo.getUuid()));
        svo.setState(PrimaryStorageState.Enabled);
        svo.setStatus(PrimaryStorageStatus.Disconnected);
        svo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        svo.setSharedBlockGroupType(SharedBlockGroupType.LvmVolumeGroupBasic);
        svo = dbf.persistAndRefresh(svo);

        APIAddSharedBlockGroupPrimaryStorageMsg bmsg = (APIAddSharedBlockGroupPrimaryStorageMsg) msg;

        List<SharedBlockVO> sharedBlockVOS = new ArrayList<>();
        for (String uuid : bmsg.getDiskUuids()) {
            SharedBlockVO blockVO = new SharedBlockVO();
            blockVO.setUuid(Platform.getUuid());
            blockVO.setSharedBlockGroupUuid(vo.getUuid());
            blockVO.setDiskUuid(uuid);
            blockVO.setType(SharedBlockType.LvmLogicalVolumeBasic);
            blockVO.setName(String.format("disk-%s", uuid));
            blockVO.setState(SharedBlockState.Enabled);
            blockVO.setStatus(SharedBlockStatus.Disconnected);
            sharedBlockVOS.add(blockVO);
        }
        dbf.persistCollection(sharedBlockVOS);

        List<SharedBlockCapacityVO> sharedBlockCapacityVOS = new ArrayList<>();
        sharedBlockVOS.forEach(it -> {
            SharedBlockCapacityVO sharedBlockCapacityVO = new SharedBlockCapacityVO();
            sharedBlockCapacityVO.setUuid(it.getUuid());
            sharedBlockCapacityVOS.add(sharedBlockCapacityVO);
        });
        dbf.persistCollection(sharedBlockCapacityVOS);

        tagMgr.createNonInherentSystemTag(svo.getUuid(), SharedBlockSystemTags.SHARED_BLOCK_NOT_INITIALIZED_TOKEN,
                PrimaryStorageVO.class.getSimpleName());

        return PrimaryStorageInventory.valueOf(svo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SharedBlockGroupPrimaryStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }

    @Override
    public void validateStorageProtocol(String protocol) {

    }

    private void setupCanonicalEvents() {
        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData d =
                        (PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData)data;
                PrimaryStorageStatus psStatus = Q.New(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.uuid, d.getPrimaryStorageUuid())
                        .eq(PrimaryStorageVO_.type, SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)
                        .select(PrimaryStorageVO_.status)
                        .findValue();

                boolean recoverConnection = d.getNewStatus() == PrimaryStorageHostStatus.Connected &&
                        d.getOldStatus() != PrimaryStorageHostStatus.Connected;

                if (psStatus == null || !recoverConnection) {
                    return;
                }

                logger.debug(String.format("NFS[uuid:%s] recover connection to host[uuid:%s]", d.getPrimaryStorageUuid(), d.getHostUuid()));
                if (psStatus != PrimaryStorageStatus.Connected) {
                    // use sync call here to make sure the sharedblock primary storage connected before continue to the next step
                    ChangePrimaryStorageStatusMsg cmsg = new ChangePrimaryStorageStatusMsg();
                    cmsg.setPrimaryStorageUuid(d.getPrimaryStorageUuid());
                    cmsg.setStatus(PrimaryStorageStatus.Connected.toString());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, d.getPrimaryStorageUuid());
                    bus.call(cmsg);
                    logger.debug(String.format("connect nfs[uuid:%s] completed", d.getPrimaryStorageUuid()));
                }

                calculatePrimaryStorageCapacity(d.getPrimaryStorageUuid());
            }
        });
    }

    private void calculatePrimaryStorageCapacity(String psUuid) {
        RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg);
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        final HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(paramIn.getSnapshot().getFormat());

        class Context {
            String temporaryInstallPath;
        }

        final Context ctx = new Context();

        template.setCreateTemporaryTemplate(new Flow() {
            String __name__ = "create-temporary-template";

            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                CreateTemporaryVolumeFromSnapshotMsg msg = new CreateTemporaryVolumeFromSnapshotMsg();
                msg.setHypervisorType(hvType.toString());
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setTemporaryVolumeUuid(paramIn.getImage().getUuid());
                msg.setSnapshot(paramIn.getSnapshot());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            ParamOut paramOut = (ParamOut) data.get(ParamOut.class);
                            CreateTemporaryVolumeFromSnapshotReply ar = reply.castReply();
                            ctx.temporaryInstallPath = ar.getInstallPath();
                            paramOut.setSize(ar.getSize());
                            paramOut.setActualSize(ar.getActualSize());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.temporaryInstallPath != null) {
                    DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                    msg.setHypervisorType(hvType.toString());
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setInstallPath(ctx.temporaryInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                ask.setImageUuid(paramIn.getImage().getUuid());
                ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                ask.setImageMediaType(paramIn.getImage().getMediaType());
                bus.makeLocalServiceId(ask, BackupStorageConstant.SERVICE_ID);
                MessageReply ar = bus.call(ask);
                if (!ar.isSuccess()) {
                    trigger.fail(ar.getError());
                    return;
                }

                String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setHypervisorType(hvType.toString());
                msg.setPrimaryStorageInstallPath(ctx.temporaryInstallPath);
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                msg.setBackupStorageInstallPath(bsInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());


                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            UploadBitsToBackupStorageReply r = reply.castReply();
                            out.setBackupStorageInstallPath(r.getBackupStorageInstallPath());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NoRollbackFlow() {
            String __name__ = "delete-temporary-template";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setInstallPath(ctx.temporaryInstallPath);
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setHypervisorType(hvType.toString());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        });

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void preDeleteHost(HostInventory inventory) {
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        final List<String> psUuids = getSharedBlockGroupPrimaryStorageInCluster(inventory.getClusterUuid());
        if(psUuids == null || psUuids.isEmpty()) {
            return;
        }

        DisconnectSharedBlockGroupPrimaryStorageOnHostMsg msg = new DisconnectSharedBlockGroupPrimaryStorageOnHostMsg();
        msg.setHostUuid(inventory.getUuid());
        msg.setHypervisorType(inventory.getHypervisorType());

        FutureCompletion completion = new FutureCompletion(null);
        ErrorCodeList errList = new ErrorCodeList();
        new While<>(psUuids).each((String psUuid, WhileCompletion whileCompletion) -> {
            msg.setPrimaryStorageUuid(psUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        String clusterUuid = inventory.getClusterUuid();
        Long exisingHosts = SQL.New("select count(hostRef)" +
                " from PrimaryStorageClusterRefVO clusterRef, SharedBlockGroupPrimaryStorageHostRefVO hostRef" +
                " where clusterRef.clusterUuid = :cuuid" +
                " and clusterRef.primaryStorageUuid = hostRef.primaryStorageUuid", Long.class)
                .param("cuuid", clusterUuid).find();

        if (exisingHosts > 0) {
            return;
        }

        final List<String> psUuids = getSharedBlockGroupPrimaryStorageInCluster(clusterUuid);
        if(psUuids == null || psUuids.isEmpty()) {
            return;
        }

        for (String psUuid : psUuids) {
            releasePrimaryStorageCapacity(psUuid);
        }
    }

    public List<String> getSharedBlockGroupPrimaryStorageInCluster(String clusterUuid) {
        return SQL.New("select sbg.uuid" +
                " from PrimaryStorageClusterRefVO clusterRef, SharedBlockGroupVO sbg" +
                " where clusterRef.clusterUuid = :cuuid" +
                " and clusterRef.primaryStorageUuid = sbg.uuid",  String.class)
                .param("cuuid", clusterUuid)
                .list();
    }

    private void releasePrimaryStorageCapacity(String psUuid) {
        RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg);
    }

    @Transactional
    public List<HostInventory> getConnectedHostsForOperation(PrimaryStorageInventory pri, int startPage, int pageLimit, Boolean raiseException) {
        if (pri.getAttachedClusterUuids().isEmpty()) {
            if (raiseException) {
                throw new OperationFailureException(operr("cannot find an available host to execute command for shared block group primary storage[uuid:%s]", pri.getUuid()));
            }
            return new ArrayList<>();
        }

        String sql = "select h from HostVO h " +
                "where h.status = :connectionState and h.clusterUuid in (:clusterUuids) " +
                "and h.state not in (:notAllowedHostState) " +
                "and h.uuid not in (select ref.hostUuid from SharedBlockGroupPrimaryStorageHostRefVO ref " +
                "where ref.primaryStorageUuid = :psUuid and ref.hostUuid = h.uuid and ref.status in (:status))";
        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("connectionState", HostStatus.Connected);
        q.setParameter("notAllowedHostState", Arrays.asList(HostState.PreMaintenance, HostState.Maintenance));
        q.setParameter("clusterUuids", pri.getAttachedClusterUuids());
        q.setParameter("psUuid", pri.getUuid());
        q.setParameter("status", Arrays.asList(PrimaryStorageHostStatus.Disconnected, PrimaryStorageHostStatus.Connecting));

        q.setFirstResult(startPage * pageLimit);
        if (pageLimit > 0){
            q.setMaxResults(pageLimit);
        }

        List<HostVO> ret = q.getResultList();
        if (ret.isEmpty() && startPage == 0 && raiseException) { //check is first page
            throw new OperationFailureException(operr(
                    "cannot find a host which has connected shared block " +
                            "to execute command for shared block group primary storage[uuid:%s]",
                    pri.getUuid()));
        } else {
            Collections.shuffle(ret);
            return HostInventory.valueOf(ret);
        }
    }

    @Transactional
    public List<HostInventory> getConnectedHostsForOperation(PrimaryStorageInventory pri, int startPage, int pageLimit) {
        return getConnectedHostsForOperation(pri, startPage, pageLimit, true);
    }

    @Transactional
    public List<HostInventory> getConnectedHostsForOperation(PrimaryStorageInventory pri) {
        return getConnectedHostsForOperation(pri, 0, 0);
    }

    @Transactional
    public List<HostInventory> getConnectedHostsForOperation(String psUuid) {
        SharedBlockGroupVO vo = Q.New(SharedBlockGroupVO.class)
                .eq(SharedBlockGroupVO_.uuid, psUuid)
                .find();

        if (vo == null) {
            return new ArrayList<>();
        }

        return getConnectedHostsForOperation(SharedBlockGroupPrimaryStorageInventory.valueOf(vo));
    }

    @Transactional
    public List<HostInventory> getConnectedHostsForOperation(PrimaryStorageInventory pri, VolumeInventory volumeInventory) {
        List<HostInventory> hosts = getConnectedHostsForOperation(pri, 0, 0);
        CollectionUtils.shuffleByKeySeed(hosts, volumeInventory.getUuid(), HostInventory::getUuid);
        if (!volumeInventory.isAttached()) {
            return hosts;
        }

        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeInventory.getVmInstanceUuid()).find();
        if (vmvo == null || vmvo.getHostUuid() == null) {
            return hosts;
        }

        return Collections.singletonList(hosts.stream().filter(h -> h.getUuid().equals(vmvo.getHostUuid())).findFirst()
                .orElseThrow(() -> new OperationFailureException(operr("the host[uuid: %s] running on is not" +
                                " available to resize volume[uuid: %s] on shared block group primary storage[uuid: %s]",
                        vmvo.getHostUuid(), volumeInventory.getUuid(), volumeInventory.getPrimaryStorageUuid()))
                ));
    }

    public List<HostInventory> getConnectedHostsForOperation(String psUuid, VolumeInventory volumeInventory) {
        SharedBlockGroupVO vo = dbf.findByUuid(psUuid, SharedBlockGroupVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("primary storage[uuid:%s] not found", psUuid));
        }

        return getConnectedHostsForOperation(SharedBlockGroupPrimaryStorageInventory.valueOf(vo), volumeInventory);
    }

    public List<HostInventory> getConnectedHostsForOperationToleranceDeleted(PrimaryStorageInventory pri, String volUuid) {
        VolumeVO volVO = dbf.findByUuid(volUuid, VolumeVO.class);
        if (volVO == null) {
            return getConnectedHostsForOperation(pri);
        }

        return getConnectedHostsForOperation(pri, VolumeInventory.valueOf(volVO));
    }
    public List<HostInventory> getConnectedHostsForOperation(String psUuid, String volUuid) {
        VolumeVO volVO = dbf.findByUuid(volUuid, VolumeVO.class);
        if (volVO == null) {
            throw new OperationFailureException(operr("volume[uuid:%s] not found", volUuid));
        }

        return getConnectedHostsForOperation(psUuid, VolumeInventory.valueOf(volVO));
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
        if(!inventory.getType().equals(type.toString())){
            return;
        }

        PrimaryStorageVO vo = dbf.findByUuid(inventory.getUuid(), PrimaryStorageVO.class);
        if(null == vo){
            logger.warn(String.format("run afterRecalculatePrimaryStorageCapacity fail, not find ps[%s] db record", inventory.getUuid()));
            return;
        }

        SharedBlockGroupPrimaryStorageBase base = new SharedBlockGroupPrimaryStorageBase(vo);
        if (base.isUnmounted()){
            releasePrimaryStorageCapacity(inventory.getUuid());
        }
    }


    @Override
    public void afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory snapshot) {
        PrimaryStorageVO vo = dbf.findByUuid(snapshot.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (vo == null) {
            logger.warn(String.format("run afterMarkRootVolumeAsSnapshot fail, not find ps[%s] db record", snapshot.getPrimaryStorageUuid()));
            return;
        }

        if (!vo.getType().equals(type.toString())) {
            return;
        }

        VolumeVO volumeVO = dbf.findByUuid(snapshot.getVolumeUuid(), VolumeVO.class);
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(snapshot.getUuid(), VolumeSnapshotVO.class);
        snapshotVO.setPrimaryStorageInstallPath(volumeVO.getInstallPath());
        dbf.update(snapshotVO);

        FutureCompletion completion = new FutureCompletion(null);

        ShrinkVolumeSnapshotOnPrimaryStorageMsg msg = new ShrinkVolumeSnapshotOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(snapshot.getPrimaryStorageUuid());
        msg.setSnapshotUuid(snapshot.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vo.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    logger.info(String.format("shrink snapshot %s successfully afterMarkRootVolumeAsSnapshot", snapshot.getUuid()));
                    ShrinkVolumeSnapshotOnPrimaryStorageReply sreply = (ShrinkVolumeSnapshotOnPrimaryStorageReply) reply;
                    ShrinkResult shrinkResult = sreply.getShrinkResult();
                    if (shrinkResult.getDeltaSize() != 0) {
                        snapshotVO.setSize(shrinkResult.getSize());
                        dbf.update(snapshotVO);
                        releasePrimaryStorageCapacity(vo.getUuid());
                    }
                    completion.success();
                }
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            logger.warn(String.format("shrink snapshot %s failed afterMarkRootVolumeAsSnapshot beacause %s", snapshot.getUuid(), completion.getErrorCode().getDetails()));
        }
    }

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (!inventory.getType().equals(type.toString())) {
            return;
        }
        releasePrimaryStorageCapacity(inventory.getUuid());
        ConfigureFilterMsg cmsg = new ConfigureFilterMsg();
        cmsg.setClusterUuid(clusterUuid);
        cmsg.setPrimaryStorageUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
        bus.send(cmsg);
    }

    private VolumeTO convertVolumeToSharedBlockIfNeeded(VolumeInventory vol, VolumeTO to) {
        if (!vol.getInstallPath().startsWith(VolumeTO.SHAREDBLOCK)) {
            return to;
        }

        to.setInstallPath(to.getInstallPath().replace(SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME, "/dev/"));
        return to;
    }

    private String convertIsoToSharedBlockIfNeeded(String isoPath) {
        if (!isoPath.startsWith(VolumeTO.SHAREDBLOCK)) {
            return isoPath;
        }

        return isoPath.replace(SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME, "/dev/");
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeToSharedBlockIfNeeded(spec.getDestRootVolume(), cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<VolumeTO>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeToSharedBlockIfNeeded(vol, to));
                    break;
                }
            }
        }

        cmd.setDataVolumes(dtos);

        for (KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
            if (cdRomTO.isEmpty()) {
                continue;
            }
            cdRomTO.setPath(convertIsoToSharedBlockIfNeeded(cdRomTO.getPath()));
        }
   }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeToSharedBlockIfNeeded(inventory, to);
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        PrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (pvo.getType().equals(type.toString())) {
            cmd.setVolume(convertVolumeToSharedBlockIfNeeded(volume, cmd.getVolume()));
        }
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {
    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        PrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (pvo.getType().equals(type.toString())) {
            cmd.setVolume(convertVolumeToSharedBlockIfNeeded(volume, cmd.getVolume()));
        }
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {
    }

    @Override
    public void beforeMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd) {
        if (cmd.getSrcPath() == null || cmd.getDestPath() == null) {
            return;
        }

        PrimaryStorageVO pvo = dbf.findByUuid(msg.getTo().getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (pvo.getType().equals(type.toString())) {
            cmd.setSrcPath(convertInstallPathToAbsolute(cmd.getSrcPath()));
            cmd.setDestPath(convertInstallPathToAbsolute(cmd.getDestPath()));
        }
    }

    @Override
    public void afterMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd) {

    }

    @Override
    public void afterMergeSnapshotFailed(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd, ErrorCode err) {
    }

    @Override
    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        if (cmd.getVolume().getPrimaryStorageType().equals(type.toString())) {
            cmd.setVolumeInstallPath(convertInstallPathToAbsolute(cmd.getVolumeInstallPath()));
            cmd.setInstallPath(convertInstallPathToAbsolute(cmd.getInstallPath()));
        }
        completion.success();
    }

    @Override
    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp) {
        if (cmd.getVolume().getPrimaryStorageType().equals(type.toString())) {
            rsp.setNewVolumeInstallPath(convertAbsolutePathToInstall(rsp.getNewVolumeInstallPath()));
            rsp.setSnapshotInstallPath(convertAbsolutePathToInstall(rsp.getSnapshotInstallPath()));
        }
    }

    @Override
    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err) {
    }

    @Override
    public HostResizeVolumeStruct beforeKvmHostResizeVolume(HostResizeVolumeStruct struct, VolumeInventory vol, String hostUuid) {
        PrimaryStorageVO pvo = dbf.findByUuid(vol.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (pvo.getType().equals(type.toString())) {
            struct.setInstallPath(convertInstallPathToAbsolute(struct.getInstallPath()));
            return struct;
        }

        return struct;
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public boolean storageConsistencySupported() {
        return true;
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param, final Completion completion) {
        SetupSelfFencerOnKvmHostMsg msg = new SetupSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {
        CancelSelfFencerOnKvmHostMsg msg = new CancelSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public List<ScsiLunVO> filterGetScsiLunCandidates(VmInstanceInventory vm, List<ScsiLunVO> scsiLuns) {
        if (scsiLuns == null || scsiLuns.isEmpty()) {
            return scsiLuns;
        }
        List<ScsiLunVO> result = new ArrayList<>();
        for (ScsiLunVO lun : scsiLuns) {
            if (!Q.New(SharedBlockVO.class).eq(SharedBlockVO_.diskUuid, lun.getWwid()).isExists()) {
                result.add(lun);
            }
        }
        return result;
    }

    @Override
    public void beforeCheckSnapshot(KVMHostInventory host, CheckSnapshotOnHypervisorMsg msg, KVMAgentCommands.CheckSnapshotCmd cmd, Completion completion) {
        boolean isSBLK = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                .eq(PrimaryStorageVO_.type, type.toString())
                .isExists();
        if (isSBLK) {
            cmd.setVolumeChainToCheck(cmd.getVolumeChainToCheck()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> convertInstallPathToAbsolute(e.getKey()), Map.Entry::getValue)));
            cmd.setExcludeInstallPaths(cmd.getExcludeInstallPaths()
                    .stream()
                    .map(SharedBlockKvmBackend::convertInstallPathToAbsolute)
                    .collect(Collectors.toList()));
        }
        completion.success();
    }

    @Override
    public void beforeCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, Completion completion) {
        if (cmd.getVolume().getPrimaryStorageType().equals(type.toString())) {
            cmd.setTop(convertInstallPathToAbsolute(cmd.getTop()));
            cmd.setBase(convertInstallPathToAbsolute(cmd.getBase()));
            cmd.setTopChildrenInstallPathInDb(convertInstallPathsToAbsolute(cmd.getTopChildrenInstallPathInDb()));
        }

        completion.success();
    }

    @Override
    public void afterCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, CommitVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        completion.success();
    }

    @Override
    public void failedToCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, KVMAgentCommands.BlockCommitResponse rsp, ErrorCode err) {

    }

    @Override
    public void beforePullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, Completion completion) {
        if (cmd.getVolume().getPrimaryStorageType().equals(type.toString())) {
            cmd.setBase(cmd.getBase() == null ? cmd.getBase() : convertInstallPathToAbsolute(cmd.getBase()));
        }

        completion.success();
    }

    @Override
    public void afterPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, PullVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        completion.success();
    }

    @Override
    public void failedToPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, KVMAgentCommands.BlockPullResponse rsp, ErrorCode err) {

    }
}
