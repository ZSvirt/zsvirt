package org.zstack.storage.addon.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.singleflight.MultiNodeSingleFlightImpl;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostResizeVolumeExtensionPoint;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostResizeVolumeStruct;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.addon.backup.ExternalBackupStorageFactory;
import org.zstack.storage.primary.PrimaryStorageFeatureAllocatorExtensionPoint;
import org.zstack.storage.snapshot.MarkRootVolumeAsSnapshotExtension;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class ExternalPrimaryStorageFactory implements PrimaryStorageFactory, Component, PSCapacityExtensionPoint,
        PreVmInstantiateResourceExtensionPoint, VmReleaseResourceExtensionPoint,
        VmAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint, BeforeTakeLiveSnapshotsOnVolumes,
        CreateTemplateFromVolumeSnapshotExtensionPoint, MarkRootVolumeAsSnapshotExtension, VmInstanceMigrateExtensionPoint,
        ManagementNodeChangeListener, PrimaryStorageFeatureAllocatorExtensionPoint, HostResizeVolumeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorageFactory.class);

    public static PrimaryStorageType type = new PrimaryStorageType(PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE);

    protected static Map<String, PrimaryStorageControllerSvc> controllers = new HashMap<>();
    protected static Map<String, PrimaryStorageNodeSvc> nodes = new HashMap<>();
    private static final List<String> SUPPORT_PROTOCOL = Arrays.asList("Vhost", "iSCSI", "NVMEoF", "CBD", "file");

    public Map<String, NodeHealthyCheckProtocolExtensionPoint> nodeHealthyCheckProtocolExtensions = Collections.synchronizedMap(
            new HashMap<String, NodeHealthyCheckProtocolExtensionPoint>());

    public Map<String, BlockExternalPrimaryStorageFactory> blockExternalPrimaryStorageFactories = Collections.synchronizedMap(
            new HashMap<String, BlockExternalPrimaryStorageFactory>());

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected EventFacade evtf;

    static {
        type.setSupportHeartbeatFile(true);
        type.setSupportStorageTrash(true);
        type.setSupportSharedVolume(true);
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(ExternalPrimaryStorageSvcBuilder.class, ExternalPrimaryStorageSvcBuilder::getIdentity);
        buildPsController();
        populateExtensions();
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                ConnectPrimaryStorageMsg cmsg = (ConnectPrimaryStorageMsg) msg;
                if (cmsg.isNewAdded() && !controllers.containsKey(cmsg.getPrimaryStorageUuid())) {
                    ExternalPrimaryStorageVO evo = dbf.findByUuid(cmsg.getPrimaryStorageUuid(), ExternalPrimaryStorageVO.class);
                    if (evo != null) {
                        logger.debug("receive connect ps msg, save external ps controller if need");
                        saveControllerIfNeed(evo);
                    }
                }
            }
        }, ConnectPrimaryStorageMsg.class);

        bus.subscribeEvent(e -> {
            if (e instanceof APIAddPrimaryStorageEvent) {
                APIAddPrimaryStorageEvent evt = (APIAddPrimaryStorageEvent) e;
                if (evt.isSuccess() && evt.getInventory() instanceof ExternalPrimaryStorageInventory) {
                    logger.debug("receive event, save external ps controller if need");
                    saveControllerIfNeed(dbf.findByUuid(evt.getInventory().getUuid(), ExternalPrimaryStorageVO.class));
                }
            }

            return false;
        }, new APIAddPrimaryStorageEvent());

        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData d = (PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData) data;
                MultiNodeSingleFlightImpl.unregister(d.getPrimaryStorageUuid());
            }
        });

        return true;
    }

    private void buildPsController() {
        List<ExternalPrimaryStorageVO> extPs = dbf.listAll(ExternalPrimaryStorageVO.class);
        for (ExternalPrimaryStorageVO vo : extPs) {
            if (!controllers.containsKey(vo.getUuid())) {
                saveControllerIfNeed(vo);
            }
        }
    }

    private void populateExtensions() {
        for (NodeHealthyCheckProtocolExtensionPoint ext : pluginRgty.getExtensionList(NodeHealthyCheckProtocolExtensionPoint.class)) {
            NodeHealthyCheckProtocolExtensionPoint old = nodeHealthyCheckProtocolExtensions.get(ext.getHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NodeHealthyCheckProtocolExtensionPoint[%s, %s] for hypervisor type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHypervisorType()));
            }
            nodeHealthyCheckProtocolExtensions.put(ext.getHypervisorType().toString(), ext);
        }

        for (BlockExternalPrimaryStorageFactory factory : pluginRgty.getExtensionList(BlockExternalPrimaryStorageFactory.class)) {
            BlockExternalPrimaryStorageFactory old = blockExternalPrimaryStorageFactories.get(factory.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BlockExternalPrimaryStorageFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), factory.getClass().getName(), factory.getType()));
            }
            blockExternalPrimaryStorageFactories.put(factory.getType(), factory);
        }
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv) {
        PrimaryStorageControllerSvc controller = controllers.get(psInv.getUuid());
        if (controller == null) {
            return psInv.getUrl();
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(true);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        return controller.allocateSpace(aspec);
    }

    @Override
    public long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid) {
        PrimaryStorageControllerSvc controller = controllers.get(psUuid);
        if (controller == null) {
            return size;
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(false);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        controller.allocateSpace(aspec);
        return size;
    }

    @Override
    public void releaseCapacity(String allocatedInstallUrl, long size, String psUuid) {

    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddExternalPrimaryStorageMsg amsg = (APIAddExternalPrimaryStorageMsg) msg;
        String identity = amsg.getIdentity();
        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(identity);
        if (builder == null) {
            throw new OperationFailureException(
                    Platform.operr("No primary storage plugin registered with identity: %s", identity)
            );
        }

        final ExternalPrimaryStorageVO lvo = new ExternalPrimaryStorageVO(vo);
        lvo.setIdentity(identity);
        lvo.setDefaultProtocol(amsg.getDefaultOutputProtocol());
        lvo.setConfig(amsg.getConfig());
        lvo.setMountPath(identity);
        PrimaryStorageOutputProtocolRefVO ref = new PrimaryStorageOutputProtocolRefVO();
        ref.setPrimaryStorageUuid(lvo.getUuid());
        ref.setOutputProtocol(amsg.getDefaultOutputProtocol());
        lvo.getOutputProtocols().add(ref);
        dbf.persist(lvo);
        dbf.persist(ref);

        saveControllerIfNeed(lvo);
        return lvo.toInventory();
    }

    boolean saveControllerIfNeed(ExternalPrimaryStorageVO extVO) {
        if (controllers.containsKey(extVO.getUuid())) {
            return false;
        }

        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(extVO.getIdentity());
        PrimaryStorageControllerSvc controller = builder.buildControllerSvc(extVO);
        controllers.put(extVO.getUuid(), controller);

        if (controller instanceof PrimaryStorageNodeSvc) {
            nodes.put(extVO.getUuid(), (PrimaryStorageNodeSvc) controller);
        } else {
            nodes.put(extVO.getUuid(), builder.buildNodeSvc(extVO));
        }
        return true;
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new ExternalPrimaryStorage(vo, controllers.get(vo.getUuid()), nodes.get(vo.getUuid()));
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return ExternalPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, ExternalPrimaryStorageVO.class));
    }

    @Override
    public void validateStorageProtocol(String protocol) {
        if (!SUPPORT_PROTOCOL.contains(protocol)) {
            throw new ApiMessageInterceptionException(argerr("not support protocol[%s] " +
                    "on type[%s] primary storage", protocol, getPrimaryStorageType()));
        }
    }

    public PrimaryStorageControllerSvc getControllerSvc(String primaryStorageUuid) {
        return controllers.get(primaryStorageUuid);
    }

    public PrimaryStorageNodeSvc getNodeSvc(String primaryStorageUuid) {
        return nodes.get(primaryStorageUuid);
    }

    private PrimaryStorageNodeSvc getNodeSvcByVolume(VolumeInventory volumeInventory) {
        if (volumeInventory.getPrimaryStorageUuid() == null || volumeInventory.getInstallPath() == null) {
            return null;
        }

        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!support(identity)) {
            return null;
        }

        return getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    public boolean support(String identity) {
        return getSvcBuilder(identity) != null;
    }

    private ExternalPrimaryStorageSvcBuilder getSvcBuilder(String identity) {
        return pluginRgty.getExtensionFromMap(identity, ExternalPrimaryStorageSvcBuilder.class);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation().equals(VmInstanceConstant.VmOperation.ChangeImage)) {
            completion.success();
            return;
        }

        List<BaseVolumeInfo> vols = getManagerVolume(spec);

        if (vols.isEmpty()) {
            completion.success();
            return;
        }

        activateVolumes(vols, spec.getDestHost(), false, completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation().equals(VmInstanceConstant.VmOperation.ChangeImage)) {
            completion.success();
            return;
        }

        List<BaseVolumeInfo> vols = getManagerVolume(spec).stream().filter(vol -> !vol.isShareable()).collect(Collectors.toList());
        if (vols.isEmpty()) {
            completion.success();
            return;
        }

        deactivateVolumes(vols, spec.getDestHost(), completion);
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        preReleaseVmResource(spec, completion);
    }

    private void deactivateVolumes(List<BaseVolumeInfo> vols, HostInventory host, Completion completion) {

        new While<>(vols).each((vol, compl) -> {
            PrimaryStorageNodeSvc svc = getNodeSvc(vol.getPrimaryStorageUuid());
            svc.deactivate(vol.getInstallPath(), vol.getProtocol(), host, new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    // todo rollback
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private void activateVolumes(List<BaseVolumeInfo> vols, HostInventory host, boolean shareable, Completion completion) {
        new While<>(vols).each((vol, compl) -> {
            PrimaryStorageNodeSvc svc = getNodeSvc(vol.getPrimaryStorageUuid());
            svc.activate(vol, host, shareable | vol.isShareable(), new ReturnValueCompletion<ActiveVolumeTO>(compl) {
                @Override
                public void success(ActiveVolumeTO v) {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private List<BaseVolumeInfo> getManagerVolume(VmInstanceSpec spec) {
        List<BaseVolumeInfo> vols = new ArrayList<>();
        // TODO how dest root volume is null
        if (spec.getDestRootVolume() != null) {
            vols.add(BaseVolumeInfo.valueOf(spec.getDestRootVolume()));
        }
        spec.getDestDataVolumes().forEach(vol -> vols.add(BaseVolumeInfo.valueOf(vol)));

        spec.getCdRomSpecs().forEach(cdRomSpec -> {
            if (cdRomSpec.getInstallPath() != null && cdRomSpec.getProtocol() != null) {
                vols.add(BaseVolumeInfo.valueOf(cdRomSpec));
            }
        });

        vols.removeIf(info -> {
            if (info.getInstallPath() == null) {
                return true;
            }
            String identity = info.getInstallPath().split("://")[0];
            return !support(identity);
        });

        return vols;
    }

    private List<BaseVolumeInfo> getManagerVolume(VmInstanceInventory vm) {
        List<BaseVolumeInfo> vols = new ArrayList<>();

        vm.getAllDiskVolumes().forEach(vol -> vols.add(BaseVolumeInfo.valueOf(vol)));

        if (getNodeSvc(vm.getRootVolume().getPrimaryStorageUuid()) != null) {
            List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class).eq(VmCdRomVO_.vmInstanceUuid, vm.getUuid()).list();
            cdRomVOS.forEach(cdRomVO -> {
                if (cdRomVO.getIsoUuid() != null) {
                    BaseVolumeInfo info = BaseVolumeInfo.valueOf(cdRomVO);
                    info.setPrimaryStorageUuid(vm.getRootVolume().getPrimaryStorageUuid());
                    vols.add(info);
                }
            });
        }

        vols.removeIf(info -> {
            if (info.getInstallPath() == null) {
                return true;
            }
            String identity = info.getInstallPath().split("://")[0];
            return !support(identity);
        });

        return vols;
    }

    private void activeVolumeIfNeed(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        PrimaryStorageNodeSvc svc = getNodeSvcByVolume(volume);
        if (svc == null) {
            completion.success();
            return;
        }

        if (vm.getHostUuid() == null || VmInstanceState.Stopped.toString().equals(vm.getState())) {
            completion.success();
            return;
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        svc.activate(BaseVolumeInfo.valueOf(volume), host, volume.isShareable(), new ReturnValueCompletion<ActiveVolumeTO>(completion) {
            @Override
            public void success(ActiveVolumeTO returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void deactivateVolumeIfNeed(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        PrimaryStorageNodeSvc svc = getNodeSvcByVolume(volume);
        if (svc == null || vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        // TODO change async interface
        svc.deactivate(volume.getInstallPath(), volume.getProtocol(), host, completion);
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        activeVolumeIfNeed(vm, volume, completion);
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {}

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        activeVolumeIfNeed(vm, volume, completion);
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {
        deactivateVolumeIfNeed(vm, volume, new NopeCompletion());
    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        deactivateVolumeIfNeed(vm, volume, completion);
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {}

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmMsg otmsg, Map flowData, Completion completion) {
        List<CreateVolumesSnapshotsJobStruct> storageSnapshots = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct struct : msg.getVolumeSnapshotJobs()) {
            PrimaryStorageControllerSvc svc = getControllerSvc(struct.getPrimaryStorageUuid());
            if (svc != null && !svc.reportCapabilities().getSnapshotCapability().isSupportCreateOnHypervisor()) {
                storageSnapshots.add(struct);
                otmsg.getSnapshotJobs().removeIf(job -> job.getVolumeUuid().equals(struct.getVolumeUuid()));
            }
        }

        if (storageSnapshots.isEmpty()) {
            completion.success();
            return;
        }

        if (otmsg.getSnapshotJobs().isEmpty()) {
            flowData.put(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR, false);
            flowData.put(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR, false);
        } else if (msg.getConsistentType() != ConsistentType.None) {
            completion.fail(operr("not support take volumes snapshots " +
                    "on multiple ps when including storage snapshot"));
            return;
        }

        logger.info(String.format("take snapshots for volumes[%s] on %s",
                msg.getLockedVolumeUuids(), getClass().getCanonicalName()));

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(storageSnapshots).all((struct, whileCompletion) -> {
            VolumeSnapshotVO vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, struct.getResourceUuid()).find();
            if (vo.getStatus().equals(VolumeSnapshotStatus.Ready)) {
                logger.warn(String.format("snapshot %s on volume %s is ready, no need to create again!",
                        vo.getUuid(), vo.getVolumeUuid()));
                whileCompletion.done();
                return;
            }
            TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
            tmsg.setPrimaryStorageUuid(struct.getPrimaryStorageUuid());
            tmsg.setStruct(struct.getVolumeSnapshotStruct());
            bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, tmsg.getPrimaryStorageUuid());
            bus.send(tmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }
                    TakeSnapshotReply treply = reply.castReply();
                    if (!treply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }

                    vo.setPrimaryStorageInstallPath(treply.getInventory().getPrimaryStorageInstallPath());
                    vo.setSize(treply.getInventory().getSize());
                    vo.setPrimaryStorageUuid(treply.getInventory().getPrimaryStorageUuid());
                    vo.setType(treply.getInventory().getType());
                    vo.setFormat(treply.getInventory().getFormat());
                    vo.setStatus(VolumeSnapshotStatus.Ready);
                    dbf.update(vo);

                    struct.getVolumeSnapshotStruct().setCurrent(treply.getInventory());
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();


        template.setCreateTemporaryTemplate(new NopeFlow());

        template.setUploadToBackupStorage(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg();
                cmsg.setSnapshotUuid(paramIn.getSnapshot().getUuid());
                cmsg.setImageInventory(paramIn.getImage());
                cmsg.setBackupStorageUuid(paramIn.getBackupStorageUuid());

                VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(paramIn.getSnapshot().getVolumeUuid(), VolumeVO.class));
                cmsg.setVolumeInventory(vol);
                bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateTemplateFromVolumeOnPrimaryStorageReply r = reply.castReply();
                        out.setBackupStorageInstallPath(r.getTemplateBackupStorageInstallPath());
                        out.setActualSize(r.getActualSize());
                        out.setSize(vol.getSize());
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() == null) {
                    trigger.rollback();
                    return;
                }

                DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                msg.setInstallPath(out.getBackupStorageInstallPath());
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.rollback();
                    }
                });
            }
        });

        template.setDeleteTemporaryTemplate(new NopeFlow());
        return template;
    }

    @Override
    public List<Flow> markRootVolumeAsSnapshot(VolumeInventory vol, VolumeSnapshotVO vo, String accountUuid) {
        PrimaryStorageControllerSvc svc = getControllerSvc(vol.getPrimaryStorageUuid());
        if (svc == null) {
            return null;
        }

        VolumeSnapshotCapability snapCap = svc.reportCapabilities().getSnapshotCapability();
        if (snapCap.getArrangementType() == VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN) {
            return null;
        }

        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = "create-snapshot-before-reimage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setAccountUuid(accountUuid);
                cmsg.setVolumeUuid(vol.getUuid());
                cmsg.setName(vol.getName());
                cmsg.setDescription(vol.getDescription());

                bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeSnapshotReply r = (CreateVolumeSnapshotReply)reply;
                        vo.setUuid(r.getInventory().getUuid());
                        if (snapCap.isSupportCreateOnHypervisor()) {
                            vo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                        } else {
                            vo.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                        }
                        trigger.next();
                    }
                });
            }
        });
        return flows;
    }

    @Override
    public String getExtensionPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        List<BaseVolumeInfo> vols = getManagerVolume(inv);
        if (CollectionUtils.isEmpty(vols)) {
            completion.success();
            return;
        }

        List<BaseVolumeInfo> exclusiveVolumes = vols.stream().filter(vol -> !vol.isShareable()).collect(Collectors.toList());
        HostInventory srcHost = HostInventory.valueOf(dbf.findByUuid(inv.getHostUuid(), HostVO.class));
        HostInventory dstHost = HostInventory.valueOf(dbf.findByUuid(destHostUuid, HostVO.class));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-external-ps-disk-for-migrate-vm-%s", inv.getUuid()));
        chain.then(new Flow() {
            String __name__ = "share-active-exclusive-disk-on-source-host";

            @Override
            public boolean skip(Map data) {
                return exclusiveVolumes.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateVolumes(exclusiveVolumes, srcHost, true, new Completion(completion) {
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
            public void rollback(FlowRollback trigger, Map data) {
                activateVolumes(exclusiveVolumes, srcHost, false, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "share-active-exclusive-disk-on-dest-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateVolumes(vols, dstHost, true, new Completion(completion) {
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
            public void rollback(FlowRollback trigger, Map data) {
                deactivateVolumes(exclusiveVolumes, dstHost, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
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


    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {}

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        List<BaseVolumeInfo> vols = getManagerVolume(inv).stream().filter(vol -> !vol.isShareable()).collect(Collectors.toList());
        if (vols.isEmpty()) {
            completion.done();
            return;
        }
        HostInventory srcHost = HostInventory.valueOf(dbf.findByUuid(srcHostUuid, HostVO.class));
        HostInventory dstHost = HostInventory.valueOf(dbf.findByUuid(inv.getHostUuid(), HostVO.class));


        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-external-ps-disk-for-migrate-vm-%s", inv.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "deactivate-disk-on-source-host";


            @Override
            public void run(FlowTrigger trigger, Map data) {
                deactivateVolumes(vols, srcHost, new Completion(completion) {
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
            String __name__ = "active-exclusive-disk-on-dest-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateVolumes(vols, dstHost, false, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.done();
            }
        }).start();
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason, NoErrorCompletion completion) {
        if (destHostUuid == null) {
            completion.done();
            return;
        }

        List<BaseVolumeInfo> vols = getManagerVolume(inv).stream().filter(vol -> !vol.isShareable()).collect(Collectors.toList());
        if (vols.isEmpty()) {
            completion.done();
            return;
        }
        HostInventory srcHost = HostInventory.valueOf(dbf.findByUuid(inv.getHostUuid(), HostVO.class));
        HostInventory dstHost = HostInventory.valueOf(dbf.findByUuid(destHostUuid, HostVO.class));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-external-ps-disk-for-migrate-vm-%s", inv.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "deactivate-disk-on-source-host";


            @Override
            public void run(FlowTrigger trigger, Map data) {
                activateVolumes(vols, srcHost, false, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "active-exclusive-disk-on-dest-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                deactivateVolumes(vols, dstHost, new Completion(completion) {
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
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.done();
            }
        }).start();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        buildPsController();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(Set<PrimaryStorageFeature> requiredFeatures, List<PrimaryStorageVO> candidates) {
        if (requiredFeatures.contains(PrimaryStorageFeature.SHARED_VOLUME)) {
            List<PrimaryStorageVO> excludeCandidates = candidates.stream()
                    .filter(v -> PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE.equals(v.getType()))
                    .filter(v -> !(controllers.containsKey(v.getUuid()) && controllers.get(v.getUuid()).reportCapabilities().isSupportShareableVolume()))
                    .collect(Collectors.toList());

            logger.info(String.format("exclude external primary storage candidates: %s for shared volume feature", excludeCandidates));

            candidates.removeAll(excludeCandidates);
        }

        return candidates;
    }

    public HostResizeVolumeStruct beforeKvmHostResizeVolume(HostResizeVolumeStruct struct, VolumeInventory vol, String hostUuid) {
        PrimaryStorageControllerSvc controller = controllers.get(vol.getPrimaryStorageUuid());
        if (controller == null) {
            return struct;
        }

        struct.setSize(controller.alignSize(struct.getSize()));
        return struct;
    }
}
