package org.zstack.storage.primary;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.MergeQueue;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.trash.DeleteRecycleExtensionPoint;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.*;
import org.zstack.header.core.trash.CleanTrashResult;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.trash.TrashCleanupResult;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.*;
import org.zstack.storage.encrypt.VolumeEncryptedTrashCleanupHelper;
import org.zstack.storage.volume.VolumeUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public abstract class PrimaryStorageBase extends AbstractPrimaryStorage {
    private final static CLogger logger = Utils.getLogger(PrimaryStorageBase.class);

    private static final Interner<String> primaryStorageHostRefKeys = Interners.newWeakInterner();

    protected PrimaryStorageVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected JobQueueFacade jobf;
    @Autowired
    protected PrimaryStorageExtensionPointEmitter extpEmitter;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected PrimaryStoragePingTracker tracker;
    @Autowired
    protected StorageTrash trash;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    private VolumeEncryptedTrashCleanupHelper volumeEncryptedTrashCleanupHelper;
    @Autowired
    private PluginRegistry pluginRgty;

    public PrimaryStorageBase() {
    }

    public static class PhysicalCapacityUsage {
        public long totalPhysicalSize;
        public long availablePhysicalSize;
    }

    public static class ConnectParam {
        private boolean newAdded;

        public boolean isNewAdded() {
            return newAdded;
        }

        public void setNewAdded(boolean newAdded) {
            this.newAdded = newAdded;
        }
    }

    protected abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg);

    protected abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg);

    protected abstract void handle(GetInstallPathForDataVolumeDownloadMsg msg);

    protected abstract void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg);

    protected abstract void handle(DeleteBitsOnPrimaryStorageMsg msg);

    protected abstract void handle(DownloadIsoToPrimaryStorageMsg msg);

    protected abstract void handle(DeleteIsoFromPrimaryStorageMsg msg);

    protected abstract void handle(AskVolumeSnapshotCapabilityMsg msg);

    protected abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg);

    protected abstract void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg);

    protected abstract void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg);

    protected abstract void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg);

    protected abstract void handle(FlattenVolumeOnPrimaryStorageMsg msg);

    protected abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg);

    protected abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg);

    protected abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg);

    protected abstract void handle(AskInstallPathForNewSnapshotMsg msg);

    protected abstract void handle(GetPrimaryStorageResourceLocationMsg msg);

    protected abstract void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg);

    protected abstract void connectHook(ConnectParam param, Completion completion);

    protected abstract void pingHook(Completion completion);

    protected abstract void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion);

    protected abstract void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg);

    protected abstract void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg);

    protected void handle(ConvertVolumeEncryptionOnPrimaryStorageMsg msg) {
        ConvertVolumeEncryptionOnPrimaryStorageReply reply = new ConvertVolumeEncryptionOnPrimaryStorageReply();
        reply.setError(operr("primary storage[type:%s, uuid:%s] does not support volume encryption conversion",
                self.getType(), self.getUuid()));
        bus.reply(msg, reply);
    }

    public PrimaryStorageBase(PrimaryStorageVO self) {
        this.self = self;
    }

    protected PrimaryStorageInventory getSelfInventory() {
        return PrimaryStorageInventory.valueOf(self);
    }

    protected String getSyncId() {
        return String.format("primaryStorage-%s", self.getUuid());
    }

    protected static List<TrashType> trashLists = CollectionDSL.list(TrashType.MigrateVolume, TrashType.MigrateVolumeSnapshot,
            TrashType.ConvertVolumeEncryption, TrashType.ConvertVolumeSnapshotEncryption,
            TrashType.RevertVolume, TrashType.VolumeSnapshot, TrashType.ReimageVolume);

    protected void fireDisconnectedCanonicalEvent(ErrorCode reason) {
        PrimaryStorageCanonicalEvent.DisconnectedData data = new PrimaryStorageCanonicalEvent.DisconnectedData();
        data.setPrimaryStorageUuid(self.getUuid());
        data.setReason(reason);
        evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DISCONNECTED, data);
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void deleteHook() {
    }

    @Override
    public void changeStateHook(PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }
    // if new kind of storage is added , override it
    protected void checkImageIfNeedToDownload(DownloadIsoToPrimaryStorageMsg msg){
        logger.debug("check if image exist in disabled primary storage");
        if(self.getState() != PrimaryStorageState.Disabled){
            return ;
        }
        if( !Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, msg.getIsoSpec().getInventory().getUuid())
                .isExists()){

            throw new OperationFailureException(operr(
                    "cannot attach ISO to a primary storage[uuid:%s] which is disabled",
                    self.getUuid()));
        }
    }

    private class PrimaryStorageValidater{
        private boolean forbidOperationWhenPrimaryStorageDisable = false;
        private boolean forbidOperationWhenPrimaryStorageMaintenance = false;

        public PrimaryStorageValidater disable(){
            this.forbidOperationWhenPrimaryStorageDisable = true;
            return this;
        }

        public PrimaryStorageValidater maintenance(){
            this.forbidOperationWhenPrimaryStorageMaintenance = true;
            return this;
        }

        public void validate(){
            ErrorCode errorCode = new ErrorCode();
            errorCode.setCode(PrimaryStorageErrors.ALLOCATE_ERROR.toString());
            errorCode.setDescription("Operation is not permitted");
            if (forbidOperationWhenPrimaryStorageDisable && self.getState().equals(PrimaryStorageState.Disabled)) {
                String error = "Operation is not permitted when primary storage status is 'Disabled', please check primary storage status";
                errorCode.setDetails(error);
            }
            if (forbidOperationWhenPrimaryStorageMaintenance && self.getState().equals(PrimaryStorageState.Maintenance)) {
                String error = "Operation is not permitted when primary storage status is 'Maintenance', please check primary storage status";
                errorCode.setDetails(error);
            }
            if (null != errorCode.getDetails()){
                throw new OperationFailureException(errorCode);
            }
        }
    }

    private void checkPrimaryStatus(Message msg) {
        if (msg instanceof InstantiateVolumeOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof DownloadVolumeTemplateToPrimaryStorageMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof CreateTemplateFromVolumeOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof DownloadDataVolumeToPrimaryStorageMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof DeleteVolumeBitsOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        } else if (msg instanceof DeleteIsoFromPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        } else if (msg instanceof AskVolumeSnapshotCapabilityMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        } else if (msg instanceof FlattenVolumeOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        } else if (msg instanceof ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().disable().maintenance()
                    .validate();
        } else if (msg instanceof CheckVolumeSnapshotOperationOnPrimaryStorageMsg) {
            SnapshotBackendOperation operation = ((CheckVolumeSnapshotOperationOnPrimaryStorageMsg) msg).getOperation();
            if (operation == SnapshotBackendOperation.FILE_CREATION) {
                new PrimaryStorageValidater().disable().maintenance()
                        .validate();
            }
        } else if (msg instanceof RebaseVolumeBackingFileOnPrimaryStorageMsg) {
            new PrimaryStorageValidater().maintenance()
                    .validate();
        }
    }

    protected void handleLocalMessage(Message msg) {
        checkPrimaryStatus(msg);
        if (msg instanceof PrimaryStorageReportPhysicalCapacityMsg) {
            handle((PrimaryStorageReportPhysicalCapacityMsg) msg);
        } else if (msg instanceof RecalculatePrimaryStorageCapacityMsg) {
            handle((RecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof InstantiateVolumeOnPrimaryStorageMsg) {
            handle((InstantiateVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeOnPrimaryStorageMsg) {
            handle((DeleteVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteBitsOnPrimaryStorageMsg) {
            handle((DeleteBitsOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateImageCacheFromVolumeOnPrimaryStorageMsg) {
            handleBase((CreateImageCacheFromVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg) {
            handleBase((CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemplateFromVolumeOnPrimaryStorageMsg) {
            handleBase((CreateTemplateFromVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelJobOnPrimaryStorageMsg) {
            handle((CancelJobOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageDeletionMsg) {
            handle((PrimaryStorageDeletionMsg) msg);
        } else if (msg instanceof DetachPrimaryStorageFromClusterMsg) {
            handle((DetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof DownloadDataVolumeToPrimaryStorageMsg) {
            handleBase((DownloadDataVolumeToPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeBitsOnPrimaryStorageMsg) {
            handle((DeleteVolumeBitsOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ConnectPrimaryStorageMsg) {
            handle((ConnectPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadIsoToPrimaryStorageMsg) {
            handleBase((DownloadIsoToPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteIsoFromPrimaryStorageMsg) {
            handle((DeleteIsoFromPrimaryStorageMsg) msg);
        } else if (msg instanceof AskVolumeSnapshotCapabilityMsg) {
            handle((AskVolumeSnapshotCapabilityMsg) msg);
        } else if (msg instanceof SyncVolumeSizeOnPrimaryStorageMsg) {
            handle((SyncVolumeSizeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof EstimateVolumeTemplateSizeOnPrimaryStorageMsg) {
            handle((EstimateVolumeTemplateSizeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BatchSyncVolumeSizeOnPrimaryStorageMsg) {
            handle((BatchSyncVolumeSizeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PingPrimaryStorageMsg) {
            handle((PingPrimaryStorageMsg) msg);
        } else if (msg instanceof ChangePrimaryStorageStatusMsg) {
            handle((ChangePrimaryStorageStatusMsg) msg);
        } else if (msg instanceof ReconnectPrimaryStorageMsg) {
            handle((ReconnectPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof FlattenVolumeOnPrimaryStorageMsg) {
            handle((FlattenVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof UpdatePrimaryStorageHostStatusMsg) {
            handle((UpdatePrimaryStorageHostStatusMsg) msg);
        } else if (msg instanceof GetInstallPathForDataVolumeDownloadMsg) {
            handle((GetInstallPathForDataVolumeDownloadMsg) msg);
        } else if (msg instanceof AskInstallPathForNewSnapshotMsg) {
            handle((AskInstallPathForNewSnapshotMsg) msg);
        } else if ((msg instanceof SyncPrimaryStorageCapacityMsg)) {
            handle((SyncPrimaryStorageCapacityMsg) msg);
        } else if ((msg instanceof DetachIsoOnPrimaryStorageMsg)) {
            handle((DetachIsoOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadVolumeTemplateToPrimaryStorageMsg) {
            handle((DownloadVolumeTemplateToPrimaryStorageMsg) msg);
        } else if ((msg instanceof CheckInstallPathInTrashMsg)) {
            handle((CheckInstallPathInTrashMsg) msg);
        } else if ((msg instanceof CleanUpTrashOnPrimaryStroageMsg)) {
            handle((CleanUpTrashOnPrimaryStroageMsg) msg);
        } else if ((msg instanceof GetVolumeSnapshotSizeOnPrimaryStorageMsg)) {
            handle((GetVolumeSnapshotSizeOnPrimaryStorageMsg) msg);
        } else if ((msg instanceof CleanUpTrashOnPrimaryStorageMsg)) {
            handle((CleanUpTrashOnPrimaryStorageMsg) msg);
        } else if ((msg instanceof GetPrimaryStorageResourceLocationMsg)) {
            handle((GetPrimaryStorageResourceLocationMsg) msg);
        } else if (msg instanceof CheckVolumeSnapshotOperationOnPrimaryStorageMsg) {
            handleBase((CheckVolumeSnapshotOperationOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ShrinkVolumeSnapshotOnPrimaryStorageMsg) {
            handle((ShrinkVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CheckChangeVolumeTypeOnPrimaryStorageMsg) {
            handle((CheckChangeVolumeTypeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ChangeVolumeTypeOnPrimaryStorageMsg) {
            handle((ChangeVolumeTypeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof UnlinkBitsOnPrimaryStorageMsg) {
            handle((UnlinkBitsOnPrimaryStorageMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotEncryptedOnPrimaryStorageMsg) {
            handle((GetVolumeSnapshotEncryptedOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ConvertVolumeEncryptionOnPrimaryStorageMsg) {
            handle((ConvertVolumeEncryptionOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeChainOnPrimaryStorageMsg) {
            handle((DeleteVolumeChainOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanUpStorageTrashOnPrimaryStorageMsg) {
            handle((CleanUpStorageTrashOnPrimaryStorageMsg)msg);
        } else if (msg instanceof UpdateVmInstanceMetadataOnPrimaryStorageMsg) {
            handle((UpdateVmInstanceMetadataOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ScanVmInstanceMetadataFromPrimaryStorageMsg) {
            handle((ScanVmInstanceMetadataFromPrimaryStorageMsg) msg);
        } else if (msg instanceof GetVmInstanceMetadataFromPrimaryStorageMsg) {
            handle((GetVmInstanceMetadataFromPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanupVmInstanceMetadataOnPrimaryStorageMsg) {
            handle((CleanupVmInstanceMetadataOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanupAllVmMetadataOnPrimaryStorageMsg) {
            handle((CleanupAllVmMetadataOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RebaseVolumeBackingFileOnPrimaryStorageMsg) {
            handle((RebaseVolumeBackingFileOnPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(DeleteVolumeChainOnPrimaryStorageMsg msg) {
        DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeFormat());
        new While<>(msg.getInstallPaths()).each((path, c) -> {
            DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
            dmsg.setInstallPath(path);
            dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
            dmsg.setHostUuid(msg.getHostUuid());
            dmsg.setHypervisorType(hvType.toString());
            bus.makeLocalServiceId(dmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(dmsg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                        c.allDone();
                        return;
                    }

                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                }

                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(CleanUpStorageTrashOnPrimaryStorageMsg msg) {
        throw new OperationFailureException(operr("operation not supported"));
    }

    protected void handle(final CleanUpTrashOnPrimaryStroageMsg msg) {
        MessageReply reply = new MessageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            private final String name = String.format("cleanup-trash-on-%s", self.getUuid());

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                cleanUpTrash(msg.getTrashId(), new ReturnValueCompletion<List<TrashCleanupResult>>(msg) {
                    @Override
                    public void success(List<TrashCleanupResult> returnValues) {
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
                return name;
            }
        });
    }

    protected void handle(final CheckInstallPathInTrashMsg msg) {
        CheckInstallPathInTrashReply reply = new CheckInstallPathInTrashReply();
        Long trashId = trash.getTrashId(self.getUuid(), msg.getInstallPath());

        if (trashId != null) {
            InstallPathRecycleInventory inv = trash.getTrash(trashId);
            reply.setTrashId(trashId);
            reply.setResourceUuid(inv.getResourceUuid());
        }
        reply.setRelatedTrashPaths(trash.findTrashInstallPath(msg.getInstallPath(), self.getUuid()));
        bus.reply(msg, reply);
    }

    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    protected void handle(final DetachIsoOnPrimaryStorageMsg msg) {
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    protected void handle(UpdatePrimaryStorageHostStatusMsg msg) {
        updatePrimaryStorageHostStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), msg.getStatus(), msg.getReason());
        bus.reply(msg, new UpdatePrimaryStorageHostStatusReply());
    }

    protected void updatePrimaryStorageHostStatus(String psUuid, String hostUuid, PrimaryStorageHostStatus newStatus, ErrorCode reason){
        synchronized (primaryStorageHostRefKeys.intern(String.format("%s-%s", psUuid, hostUuid))) {
            List<PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData> datas = new ArrayList<>();

            new SQLBatch(){
                @Override
                protected void scripts() {
                    PrimaryStorageHostStatus oldStatus = Q.New(PrimaryStorageHostRefVO.class)
                            .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                            .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                            .select(PrimaryStorageHostRefVO_.status)
                            .findValue();

                    if (oldStatus == newStatus) {
                        return;
                    }

                    if (oldStatus == null) {
                        PrimaryStorageHostRefVO ref = new PrimaryStorageHostRefVO();
                        ref.setHostUuid(hostUuid);
                        ref.setPrimaryStorageUuid(psUuid);
                        ref.setStatus(newStatus);
                        persist(ref);
                    } else {
                        sql(PrimaryStorageHostRefVO.class)
                                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                                .set(PrimaryStorageHostRefVO_.status, newStatus)
                                .update();
                    }

                    logger.debug(String.format("change status between primary storage[uuid:%s]" +
                                    " and host[uuid:%s] from %s to %s in db",
                            psUuid, hostUuid, oldStatus == null ? "unknown" : oldStatus.toString(), newStatus));

                    PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data =
                            new PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData();
                    data.setHostUuid(hostUuid);
                    data.setPrimaryStorageUuid(psUuid);
                    data.setNewStatus(newStatus);
                    data.setOldStatus(oldStatus);
                    data.setReason(reason);
                    datas.add(data);
                }
            }.execute();

            datas.forEach(it -> evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, it));
        }
    }

    protected void handle(RecalculatePrimaryStorageCapacityMsg msg) {
        RecalculatePrimaryStorageCapacityReply reply = new RecalculatePrimaryStorageCapacityReply();
        PrimaryStorageCapacityRecalculator recalculator = new PrimaryStorageCapacityRecalculator();
        recalculator.psUuids = Collections.singletonList(msg.getPrimaryStorageUuid());
        new MergeQueue().addTask(String.format("recalculate primary storage capacity: %s", msg.getPrimaryStorageUuid()), new Supplier<Void>() {
            @Override
            public Void get() {
                recalculator.recalculate();
                return null;
            }
        }).run();
        bus.reply(msg, reply);
    }

    protected void handle(ReconnectPrimaryStorageMsg msg) {
        ReconnectPrimaryStorageReply reply = new ReconnectPrimaryStorageReply();
        doConnect(new ConnectParam(), new Completion(msg) {
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

    private void handle(ChangePrimaryStorageStatusMsg msg) {
        changeStatus(PrimaryStorageStatus.valueOf(msg.getStatus()));
        ChangePrimaryStorageStatusReply reply = new ChangePrimaryStorageStatusReply();
        bus.reply(msg, reply);
    }

    private void handle(final PingPrimaryStorageMsg msg) {
        final PingPrimaryStorageReply reply = new PingPrimaryStorageReply();

        pingHook(new Completion(msg) {
            @Override
            public void success() {
                if (self.getStatus() == PrimaryStorageStatus.Disconnected) {
                    doConnect(new ConnectParam(), new NopeCompletion());
                }

                reply.setConnected(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (changeStatus(PrimaryStorageStatus.Disconnected)) {
                    fireDisconnectedCanonicalEvent(errorCode);
                }

                reply.setConnected(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleBase(DownloadIsoToPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid());
        checkImageIfNeedToDownload(msg);
        handle(msg);
    }

    private void doConnect(ConnectParam param, final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                changeStatus(PrimaryStorageStatus.Connecting);

                connectHook(param, new Completion(chain, completion) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        changeStatus(PrimaryStorageStatus.Connected);
                        logger.debug(String.format("successfully connected primary storage[uuid:%s]", self.getUuid()));

                        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
                        rmsg.setPrimaryStorageUuid(self.getUuid());
                        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(rmsg);

                        tracker.track(self.getUuid());

                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        tracker.track(self.getUuid());

                        self = dbf.reload(self);
                        if (changeStatus(PrimaryStorageStatus.Disconnected) && !errorCode.isError(PrimaryStorageErrors.DISCONNECTED)) {
                            fireDisconnectedCanonicalEvent(errorCode);
                        }

                        logger.debug(String.format("failed to connect primary storage[uuid:%s], %s", self.getUuid(), errorCode));

                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reconnect-primary-storage-%s", self.getUuid());
            }
        });
    }

    private void handle(final ConnectPrimaryStorageMsg msg) {
        final ConnectPrimaryStorageReply reply = new ConnectPrimaryStorageReply();

        ConnectParam param = new ConnectParam();
        param.newAdded = msg.isNewAdded();

        doConnect(param, new Completion(msg) {
            @Override
            public void success() {
                reply.setConnected(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (msg.isNewAdded()) {
                    reply.setError(errorCode);
                } else {
                    reply.setConnected(false);
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handleBase(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        if (self.getStatus() != PrimaryStorageStatus.Connected) {
            CheckVolumeSnapshotOperationOnPrimaryStorageReply reply = new CheckVolumeSnapshotOperationOnPrimaryStorageReply();
            reply.setError(err(PrimaryStorageErrors.DISCONNECTED, "primary storage[uuid:%s] is not Connected", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        handle(msg);
    }

    private void handleBase(DownloadDataVolumeToPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getBackupStorageRef().getBackupStorageUuid());
        if (!msg.getBackupStorageRef().getInstallPath().startsWith("nbd://")) {
            handle(msg);
            return;
        }

        VolumeUtils.SetVolumeProvisioningStrategy(msg.getVolumeUuid(), VolumeProvisioningStrategy.ThickProvisioning);
        InstantiateVolumeOnPrimaryStorageMsg imsg = new InstantiateVolumeOnPrimaryStorageMsg();
        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        if (msg.getHostUuid() != null) {
            imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class)));
        }
        imsg.setVolume(VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class)));
        imsg.setSkipIfExisting(false);
        imsg.setAllocatedInstallUrl(msg.getAllocatedInstallUrl());
        bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                DownloadDataVolumeToPrimaryStorageReply r = new DownloadDataVolumeToPrimaryStorageReply();
                if (!reply.isSuccess()) {
                    r.setError(reply.getError());
                } else {
                    InstantiateVolumeOnPrimaryStorageReply r2 = reply.castReply();
                    r.setFormat(r2.getVolume().getFormat());
                    r.setInstallPath(String.format("%s?r=%s",
                            r2.getVolume().getInstallPath(),
                            msg.getBackupStorageRef().getInstallPath())
                    );
                    r.setProtocol(r2.getVolume().getProtocol());
                }
                bus.reply(msg, r);
            }
        });
    }

    @Transactional(readOnly = true)
    private void checkIfBackupStorageAttachedToMyZone(String bsUuid) {
        String sql = "select bs.uuid" +
                " from BackupStorageVO bs, BackupStorageZoneRefVO ref" +
                " where bs.uuid = ref.backupStorageUuid" +
                " and ref.zoneUuid = :zoneUuid" +
                " and bs.uuid = :bsUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("zoneUuid", self.getZoneUuid());
        q.setParameter("bsUuid", bsUuid);
        if (q.getResultList().isEmpty()) {
            throw new OperationFailureException(operr("backup storage[uuid:%s] is not attached to zone[uuid:%s] the primary storage[uuid:%s] belongs to",
                            bsUuid, self.getZoneUuid(), self.getUuid()));
        }
    }

    protected void check(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        checkVolumeNotActive(msg.getVolumeInventory().getUuid());
    }

    protected void check(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        checkVolumeNotActive(msg.getVolumeInventory().getUuid());
    }

    private void checkVolumeNotActive(String volUuid) {
        VmInstanceState vmState = SQL.New("select vm.state from VmInstanceVO vm, VolumeVO volume" +
                " where volume.uuid = :volUuid" +
                " and volume.vmInstanceUuid = vm.uuid", VmInstanceState.class)
                .param("volUuid", volUuid)
                .find();
        if (vmState != null && vmState != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] has been attached a %s VM. VM should be Stopped.", volUuid, vmState));
        }
    }

    private void handleBase(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        new PrimaryStorageValidater().disable().maintenance()
                .validate();
        check(msg);
        handle(msg);
    }

    private void handleBase(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        new PrimaryStorageValidater().disable().maintenance()
                .validate();
        handle(msg);
    }

    private void handleBase(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        checkIfBackupStorageAttachedToMyZone(msg.getBackupStorageUuid());
        new PrimaryStorageValidater().disable().maintenance()
                .validate();
        check(msg);
        handle(msg);
    }

    protected void handle(CancelJobOnPrimaryStorageMsg msg) {
        CancelJobOnPrimaryStorageReply reply = new CancelJobOnPrimaryStorageReply();
        CancelHostTasksMsg cmsg = new CancelHostTasksMsg();
        cmsg.setCancellationApiId(msg.getCancellationApiId());
        bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final DetachPrimaryStorageFromClusterMsg msg) {
        final DetachPrimaryStorageFromClusterReply reply = new DetachPrimaryStorageFromClusterReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                extpEmitter.beforeDetach(self, msg.getClusterUuid());
                detachHook(msg.getClusterUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        self = dbf.reload(self);
                        extpEmitter.afterDetach(self, msg.getClusterUuid());

                        logger.debug(String.format("successfully detached primary storage[name: %s, uuid:%s]",
                                self.getName(), self.getUuid()));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        extpEmitter.failToDetach(self, msg.getClusterUuid());
                        logger.warn(errorCode.getReadableDetails());
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detach-primary-storage-%s-from-%s", self.getUuid(), msg.getClusterUuid());
            }
        });
    }

    private void handle(PrimaryStorageDeletionMsg msg) {
        PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        tracker.untrack(self.getUuid());
        PrimaryStorageDeletionReply reply = new PrimaryStorageDeletionReply();
        bus.reply(msg, reply);
    }

    @Transactional
    private void updateCapacity(long total, long avail) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class,
                self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        DebugUtils.Assert(cvo != null, String.format("how can there is no PrimaryStorageCapacityVO[uuid:%s]", self.getUuid()));

        cvo.setTotalPhysicalCapacity(total);
        cvo.setAvailablePhysicalCapacity(avail);
        dbf.getEntityManager().merge(cvo);
    }

    private void handle(PrimaryStorageReportPhysicalCapacityMsg msg) {
        updateCapacity(msg.getTotalCapacity(), msg.getAvailableCapacity());
        bus.reply(msg, new MessageReply());
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeletePrimaryStorageMsg) {
            handle((APIDeletePrimaryStorageMsg) msg);
        } else if (msg instanceof APIChangePrimaryStorageStateMsg) {
            handle((APIChangePrimaryStorageStateMsg) msg);
        } else if (msg instanceof APIAttachPrimaryStorageToClusterMsg) {
            handle((APIAttachPrimaryStorageToClusterMsg) msg);
        } else if (msg instanceof APIDetachPrimaryStorageFromClusterMsg) {
            handle((APIDetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof APIReconnectPrimaryStorageMsg) {
            handle((APIReconnectPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdatePrimaryStorageMsg) {
            handle((APIUpdatePrimaryStorageMsg) msg);
        } else if (msg instanceof APISyncPrimaryStorageCapacityMsg) {
            handle((APISyncPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof APICleanUpImageCacheOnPrimaryStorageMsg) {
            handle((APICleanUpImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof APICleanUpTrashOnPrimaryStorageMsg) {
            handle((APICleanUpTrashOnPrimaryStorageMsg) msg);
        } else if (msg instanceof APIGetTrashOnPrimaryStorageMsg) {
            handle((APIGetTrashOnPrimaryStorageMsg) msg);
        } else if (msg instanceof APICleanUpStorageTrashOnPrimaryStorageMsg) {
            handle((APICleanUpStorageTrashOnPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddStorageProtocolMsg) {
            handle((APIAddStorageProtocolMsg) msg);
        } else if (msg instanceof APITakeoverPrimaryStorageMsg) {
            handle((APITakeoverPrimaryStorageMsg) msg);
        } else if (msg instanceof APIScanVmInstanceMetadataFromPrimaryStorageMsg) {
            handle((APIScanVmInstanceMetadataFromPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(APITakeoverPrimaryStorageMsg msg) {
        APITakeoverPrimaryStorageEvent event = new APITakeoverPrimaryStorageEvent(msg.getId());
        event.setError(operr("takeover not supported for primary storage type[%s]", self.getType()));
        bus.publish(event);
    }

    private void handle(APIAddStorageProtocolMsg msg) {
        APIAddStorageProtocolEvent evt = new APIAddStorageProtocolEvent(msg.getId());
        addStorageProtocol(msg, new Completion(msg) {
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

    private void addStorageProtocol(APIAddStorageProtocolMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();

        chain.setName(String.format("add-protocol-to-storage-%s", msg.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "add-protocol-storage-ref-vo";
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                PrimaryStorageOutputProtocolRefVO refVO = new PrimaryStorageOutputProtocolRefVO();
                refVO.setPrimaryStorageUuid(msg.getUuid());
                refVO.setOutputProtocol(msg.getOutputProtocol());
                dbf.persist(refVO);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "do-add-protocol-for-storage-vo";
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                doAddProtocol(msg, new Completion(trigger) {
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
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    protected void doAddProtocol(APIAddStorageProtocolMsg msg, Completion completion) {
        completion.success();
    }

    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        throw new OperationFailureException(operr("operation not supported"));
    }

    private void handle(final APIGetTrashOnPrimaryStorageMsg msg) {
        APIGetTrashOnPrimaryStorageReply reply = new APIGetTrashOnPrimaryStorageReply();
        List<TrashType> lists = msg.getTrashType() != null ? CollectionDSL.list(TrashType.valueOf(msg.getTrashType())) : trashLists;
        List<InstallPathRecycleInventory> trashs = trash.getTrashList(self.getUuid(), lists);
        if (msg.getResourceUuid() == null) {
            reply.setInventories(trashs);
        } else {
            trashs.forEach(t -> {
                if (msg.getResourceUuid().equals(t.getResourceUuid()) && msg.getResourceType().equals(t.getResourceType())) {
                    reply.getInventories().add(t);
                }
            });
        }

        bus.reply(msg, reply);
    }

    protected void handle(APICleanUpStorageTrashOnPrimaryStorageMsg msg) {
        throw new OperationFailureException(operr("operation not supported"));
    }

    protected synchronized void updateTrashSize(CleanTrashResult result, Long size) {
        result.setSize(result.getSize() + size);
    }

    private void cleanTrash(Long trashId, final ReturnValueCompletion<TrashCleanupResult> completion) {
        InstallPathRecycleInventory inv = trash.getTrash(trashId);
        if (inv == null) {
            completion.success(new TrashCleanupResult(trashId));
            return;
        }

        String details = trash.makeSureInstallPathNotUsed(inv);
        if (details != null) {
//            completion.fail(operr("%s is still in using by %s, cannot remove it from trash...", inv.getInstallPath(), inv.getResourceType()));
            completion.success(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), operr(details)));
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(self.getUuid());
        msg.setInstallPath(inv.getInstallPath());
        msg.setHypervisorType(inv.getHypervisorType());
        msg.setFolder(inv.getFolder());
        msg.setBitsUuid(inv.getResourceUuid());
        msg.setBitsType(inv.getResourceType());
        msg.setHostUuid(inv.getHostUuid());
        msg.setFromRecycle(true);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info(String.format("Deleted volume %s in Trash.", inv.getInstallPath()));
                    String allocatedInstallUrl = null;
                    DeleteRecycleExtensionPoint ext = pluginRgty.getExtensionFromMap(self.getType(), DeleteRecycleExtensionPoint.class);
                    if (ext != null) {
                        allocatedInstallUrl = ext.buildAllocatedInstallUrl(inv);
                    }
                    ReleasePrimaryStorageSpaceMsg imsg = new ReleasePrimaryStorageSpaceMsg();
                    imsg.setPrimaryStorageUuid(self.getUuid());
                    imsg.setDiskSize(inv.getSize());
                    imsg.setAllocatedInstallUrl(allocatedInstallUrl);
                    bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                    bus.send(imsg);
                    volumeEncryptedTrashCleanupHelper.cleanupKeyRefAfterTrashDeleted(inv);
                    trash.removeFromDb(trashId);
                    logger.info(String.format("Returned space[size:%s] to PS %s after volume migration", inv.getSize(), self.getUuid()));
                    completion.success(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), inv.getSize()));
                } else {
                    logger.warn(String.format("Failed to delete volume %s in Trash, because: %s", inv.getInstallPath(), reply.getError().getDetails()));
                    if ("LocalStorage".equals(self.getType()) && inv.getHostUuid() == null) {
                        // to compatible old version(they did not record hostUuid)
                        trash.removeFromDb(trashId);
                        completion.success(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), 0L));
                    } else {
                        completion.fail(reply.getError());
                    }
                }
            }
        });
    }

    protected void handle(final CleanUpTrashOnPrimaryStorageMsg msg) {
        MessageReply reply = new MessageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            private final String name = String.format("cleanup-trash-on-%s", self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                cleanUpTrash(msg.getTrashId(), new ReturnValueCompletion<List<TrashCleanupResult>>(chain) {
                    @Override
                    public void success(List<TrashCleanupResult> results) {
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
                return name;
            }
        });
    }

    private void cleanUpTrash(Long trashId, final ReturnValueCompletion<List<TrashCleanupResult>> completion) {
        if (trashId != null) {
            cleanTrash(trashId, new ReturnValueCompletion<TrashCleanupResult>(completion) {
                @Override
                public void success(TrashCleanupResult res) {
                    completion.success(CollectionDSL.list(res));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        List<TrashCleanupResult> results = Collections.synchronizedList(new ArrayList<>());
        List<InstallPathRecycleInventory> trashs = trash.getTrashList(self.getUuid(), trashLists);
        if (trashs.isEmpty()) {
            completion.success(results);
            return;
        }

        new While<>(trashs).step((trash, coml) -> {
            cleanTrash(trash.getTrashId(), new ReturnValueCompletion<TrashCleanupResult>(coml) {
                @Override
                public void success(TrashCleanupResult res) {
                    results.add(res);
                    coml.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    coml.addError(errorCode);
                    coml.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success(results);
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    protected void handle(final APICleanUpTrashOnPrimaryStorageMsg msg) {
        APICleanUpTrashOnPrimaryStorageEvent evt = new APICleanUpTrashOnPrimaryStorageEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            private final String name = String.format("cleanup-trash-on-%s", self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                cleanUpTrash(msg.getTrashId(), new ReturnValueCompletion<List<TrashCleanupResult>>(chain) {
                    @Override
                    public void success(List<TrashCleanupResult> results) {
                        evt.setResult(TrashCleanupResult.buildCleanTrashResult(results));
                        evt.setResults(results);
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
                return name;
            }
        });
    }

    private void handle(final APISyncPrimaryStorageCapacityMsg msg) {
        final APISyncPrimaryStorageCapacityEvent evt = new APISyncPrimaryStorageCapacityEvent(msg.getId());

        SyncPrimaryStorageCapacityMsg smsg = new SyncPrimaryStorageCapacityMsg();
        smsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, smsg.getPrimaryStorageUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }

                SyncPrimaryStorageCapacityReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply1.getError());
                } else {
                    evt.setInventory(reply1.getInventory());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(final SyncPrimaryStorageCapacityMsg msg) {
        SyncPrimaryStorageCapacityReply reply = new SyncPrimaryStorageCapacityReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("sync-capacity-of-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "sync-physical-capacity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(trigger) {
                            @Override
                            public void success(PhysicalCapacityUsage returnValue) {
                                PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
                                updater.run(cap -> {
                                    cap.setAvailablePhysicalCapacity(returnValue.availablePhysicalSize < 0 ? 0 : returnValue.availablePhysicalSize);
                                    cap.setTotalPhysicalCapacity(returnValue.totalPhysicalSize);
                                    return cap;
                                });
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
                    String __name__ = "recalculate-primary-storage-capacity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
                        rmsg.setPrimaryStorageUuid(self.getUuid());
                        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        reply.setInventory(getSelfInventory());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setSuccess(false);
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    protected void updatePrimaryStorage(APIUpdatePrimaryStorageMsg msg, ReturnValueCompletion<PrimaryStorageInventory> completion) {
        final String name = msg.getName();
        final String des = msg.getDescription();
        final String url = msg.getUrl();
        if (name == null && des == null && url == null) {
            return;
        }
        UpdateQuery uq = SQL.New(self.getClass()).eq(PrimaryStorageVO_.uuid, self.getUuid());
        if (name != null) {
            uq.set(PrimaryStorageVO_.name, name);
        }
        if (des != null) {
            uq.set(PrimaryStorageVO_.description, des);
        }
        if (url != null) {
            uq.set(PrimaryStorageVO_.url, url);
        }
        uq.update();
        self = dbf.reload(self);
        completion.success(getSelfInventory());
    }

    private void handle(APIUpdatePrimaryStorageMsg msg) {
        APIUpdatePrimaryStorageEvent evt = new APIUpdatePrimaryStorageEvent(msg.getId());
        updatePrimaryStorage(msg, new ReturnValueCompletion<PrimaryStorageInventory>(msg) {

            @Override
            public void success(PrimaryStorageInventory returnValue) {
                evt.setInventory(returnValue);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected boolean changeStatus(PrimaryStorageStatus status) {
        self = dbf.reload(self);
        if (status == self.getStatus()) {
            return false;
        }

        PrimaryStorageStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);

        PrimaryStorageStatusChangedData d = new PrimaryStorageStatusChangedData();
        d.setInventory(PrimaryStorageInventory.valueOf(self));
        d.setPrimaryStorageUuid(self.getUuid());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(status.toString());
        evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH, d);

        logger.debug(String.format("the primary storage[uuid:%s, name:%s] changed status from %s to %s",
                self.getUuid(), self.getName(), oldStatus, status));

        return true;
    }

    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        final APIReconnectPrimaryStorageEvent evt = new APIReconnectPrimaryStorageEvent(msg.getId());

        ReconnectPrimaryStorageMsg rmsg = new ReconnectPrimaryStorageMsg();
        rmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    self = dbf.reload(self);
                    evt.setInventory(getSelfInventory());
                }

                bus.publish(evt);
            }
        });
    }

    // don't use chainTask for this method, the sub-sequential DetachPrimaryStorageFromClusterMsg
    // is in the queue
    protected void handle(final APIDetachPrimaryStorageFromClusterMsg msg) {
        final APIDetachPrimaryStorageFromClusterEvent evt = new APIDetachPrimaryStorageFromClusterEvent(msg.getId());

        try {
            extpEmitter.preDetach(self, msg.getClusterUuid());
        } catch (PrimaryStorageException e) {
            throw new OperationFailureException(err(PrimaryStorageErrors.DETACH_ERROR, e.getMessage()));
        }

        // if not, HA will allocate wrong host, rollback when API fail
        List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .list();
        dbf.removeCollection(refs, PrimaryStorageClusterRefVO.class);


        String issuer = PrimaryStorageVO.class.getSimpleName();
        List<PrimaryStorageDetachStruct> ctx = new ArrayList<>();
        PrimaryStorageDetachStruct struct = new PrimaryStorageDetachStruct();
        struct.setClusterUuid(msg.getClusterUuid());
        struct.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        ctx.add(struct);
        casf.asyncCascade(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                evt.setInventory(PrimaryStorageInventory.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //has removed RefVO before, roll back
                dbf.updateAndRefresh(refs.get(0));
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected void handle(final APIAttachPrimaryStorageToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                attachCluster(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-primary-storage-%s-to-cluster-%s", self.getUuid(), msg.getClusterUuid());
            }

            protected int getMaxPendingTasks() {
                return 0;
            }

            protected void exceedMaxPendingCallback() {
                APIAttachPrimaryStorageToClusterEvent evt = new APIAttachPrimaryStorageToClusterEvent(msg.getId());
                evt.setError(operr(getDeduplicateError(getName())));
                bus.publish(evt);
            }

            protected String getDeduplicateString() {
                return String.format("attach-primary-storage-%s-to-cluster-%s", self.getUuid(), msg.getClusterUuid());
            }
        });

    }

    private void attachCluster(final APIAttachPrimaryStorageToClusterMsg msg, final NoErrorCompletion completion) {
        final APIAttachPrimaryStorageToClusterEvent evt = new APIAttachPrimaryStorageToClusterEvent(msg.getId());
        try {
            extpEmitter.preAttach(self, msg.getClusterUuid());
        } catch (PrimaryStorageException pe) {
            evt.setError(err(PrimaryStorageErrors.ATTACH_ERROR, pe.getMessage()));
            bus.publish(evt);
            completion.done();
            return;
        }

        extpEmitter.beforeAttach(self, msg.getClusterUuid());
        attachHook(msg.getClusterUuid(), new Completion(msg, completion) {
            @Override
            public void success() {
                PrimaryStorageClusterRefVO ref = new PrimaryStorageClusterRefVO();
                ref.setClusterUuid(msg.getClusterUuid());
                ref.setPrimaryStorageUuid(self.getUuid());
                dbf.persist(ref);

                self = dbf.reload(self);
                extpEmitter.afterAttach(self, msg.getClusterUuid());

                PrimaryStorageInventory pinv = self.toInventory();
                evt.setInventory(pinv);
                logger.debug(String.format("successfully attached primary storage[name:%s, uuid:%s]",
                        pinv.getName(), pinv.getUuid()));
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                extpEmitter.failToAttach(self, msg.getClusterUuid());
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private  void stopAllVms(List<String> vmUuids) {
        final List<StopVmInstanceMsg> msgs = new ArrayList<StopVmInstanceMsg>();
        for (String vmUuid : vmUuids) {
            StopVmInstanceMsg msg = new StopVmInstanceMsg();
            msg.setVmInstanceUuid(vmUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(null) {
            @Override
            public void run(List<MessageReply> replies) {
                StringBuilder sb = new StringBuilder();
                boolean success = true;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        StopVmInstanceMsg msg = msgs.get(replies.indexOf(r));
                        String err = String.format("\nfailed to stop vm[uuid:%s] on primary storage[uuid:%s], %s",
                                msg.getVmInstanceUuid(), self.getUuid(), r.getError());
                        sb.append(err);
                        success = false;
                    }
                }

                if (!success) {
                    logger.warn(sb.toString());
                }

            }
        });

    }

    protected void handle(APIChangePrimaryStorageStateMsg msg) {
        APIChangePrimaryStorageStateEvent evt = new APIChangePrimaryStorageStateEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                self = dbf.reload(self);

                PrimaryStorageState currState = self.getState();
                PrimaryStorageStateEvent event = PrimaryStorageStateEvent.valueOf(msg.getStateEvent());
                PrimaryStorageState nextState = AbstractPrimaryStorage.getNextState(currState, event);

                try {
                    extpEmitter.preChange(self, event);
                } catch (PrimaryStorageException e) {
                    evt.setError(err(SysErrors.CHANGE_RESOURCE_STATE_ERROR, e.getMessage()));
                    bus.publish(evt);
                    return;
                }

                extpEmitter.beforeChange(self, event);
                if (PrimaryStorageStateEvent.maintain == event) {
                    logger.warn(String.format("Primary Storage %s  will enter maintenance mode, ignore unknown status VMs", msg.getPrimaryStorageUuid()));
                    List<String> vmUuids = SQL.New("select vm.uuid from VmInstanceVO vm, VolumeVO vol" +
                            " where vol.primaryStorageUuid =:uuid and vol.vmInstanceUuid = vm.uuid group by vm.uuid", String.class)
                            .param("uuid", self.getUuid()).list();
                    if ( vmUuids.size() != 0 ) {
                        stopAllVms(vmUuids);
                    }
                }
                changeStateHook(event, nextState);
                self.setState(nextState);
                self = dbf.updateAndRefresh(self);
                extpEmitter.afterChange(self, event, currState);

                PrimaryStorageCanonicalEvent.PrimaryStorageStateChangedData data = new PrimaryStorageCanonicalEvent.PrimaryStorageStateChangedData();
                data.setInventory(PrimaryStorageInventory.valueOf(self));
                data.setPrimaryStorageUuid(self.getUuid());
                data.setOldState(currState);
                data.setNewState(nextState);
                evt.setInventory(PrimaryStorageInventory.valueOf(self));
                evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATE_CHANGED_PATH, data);
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("change-primary-storage-%s-state", self.getUuid());
            }
        });
    }

    protected void handle(APIDeletePrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deletePrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-primary-storage-%s", self.getUuid());
            }
        });
    }

    private void deletePrimaryStorage(final APIDeletePrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final APIDeletePrimaryStorageEvent evt = new APIDeletePrimaryStorageEvent(msg.getId());
        self = dbf.reload(self);
        Set<PrimaryStorageClusterRefVO> pscRefs = self.getAttachedClusterRefs();
        if (!pscRefs.isEmpty()) {
            String clusterUuidsString = pscRefs.stream()
                    .map(PrimaryStorageClusterRefVO::getClusterUuid)
                    .collect(Collectors.joining(", "));
            evt.setError(operr("primary storage[uuid:%s] cannot be deleted for still " + "being attached to cluster[uuid:%s].", self.getUuid(), clusterUuidsString));
            bus.publish(evt);
            completion.done();
            return;
        }

        final String issuer = PrimaryStorageVO.class.getSimpleName();
        final List<PrimaryStorageInventory> ctx = PrimaryStorageInventory.valueOf(Collections.singletonList(self));
        self.setState(PrimaryStorageState.Deleting);
        self = dbf.updateAndRefresh(self);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-primary-storage-%s", msg.getUuid()));

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

        // Due to issue #1412, deleting PS asynchronously might leave VmInstanceEO in
        // database. Since eoCleanup() could be called before deleting VmInstanceVO.
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new Completion(trigger) {
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.publish(evt);

                PrimaryStorageDeletedData d = new PrimaryStorageDeletedData();
                d.setPrimaryStorageUuid(self.getUuid());
                d.setInventory(PrimaryStorageInventory.valueOf(self));
                evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, d);
                completion.done();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
                completion.done();
            }
        }).start();
    }

    protected void handle(GetVolumeSnapshotSizeOnPrimaryStorageMsg msg) {
        GetVolumeSnapshotSizeOnPrimaryStorageReply reply = new GetVolumeSnapshotSizeOnPrimaryStorageReply();
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        reply.setSize(snapshotVO.getSize());
        reply.setActualSize(snapshotVO.getSize());
        bus.reply(msg, reply);
    }

    protected void handle(CheckChangeVolumeTypeOnPrimaryStorageMsg msg) {
        CheckChangeVolumeTypeOnPrimaryStorageReply reply = new CheckChangeVolumeTypeOnPrimaryStorageReply();
        bus.reply(msg, reply);
    };

    protected void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg) {
        ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();
        reply.setSnapshots(msg.getSnapshots());
        reply.setVolume(msg.getVolume());
        bus.reply(msg, reply);
    };

    protected void handle(UnlinkBitsOnPrimaryStorageMsg msg) {
        UnlinkBitsOnPrimaryStorageReply reply = new UnlinkBitsOnPrimaryStorageReply();
        bus.reply(msg, reply);
    };

    protected void handle(UpdateVmInstanceMetadataOnPrimaryStorageMsg msg) {
        UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    protected void handle(GetVmInstanceMetadataFromPrimaryStorageMsg msg) {
        GetVmInstanceMetadataFromPrimaryStorageReply reply = new GetVmInstanceMetadataFromPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    protected void handle(ScanVmInstanceMetadataFromPrimaryStorageMsg msg) {
        ScanVmInstanceMetadataFromPrimaryStorageReply reply = new ScanVmInstanceMetadataFromPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    protected void handle(CleanupVmInstanceMetadataOnPrimaryStorageMsg msg) {
        CleanupVmInstanceMetadataOnPrimaryStorageReply reply = new CleanupVmInstanceMetadataOnPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    protected void handle(CleanupAllVmMetadataOnPrimaryStorageMsg msg) {
        CleanupAllVmMetadataOnPrimaryStorageReply reply = new CleanupAllVmMetadataOnPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    protected void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg) {
        RebaseVolumeBackingFileOnPrimaryStorageReply reply = new RebaseVolumeBackingFileOnPrimaryStorageReply();
        reply.setError(operr("operation not supported"));
        bus.reply(msg, reply);
    }

    // don't attach any cluster
    public boolean isUnmounted() {
        long count = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, this.self.getUuid()).count();

        return count == 0;
    }

    @VmAttachVolumeValidatorMethod
    static void vmAttachVolumeValidator(VmInstanceInventory vmInv, String volumeUuid) {
        PrimaryStorageState state = SQL.New("select pri.state from PrimaryStorageVO pri " +
                "where pri.uuid = (select vol.primaryStorageUuid from VolumeVO vol where vol.uuid = :volUuid)", PrimaryStorageState.class)
                .param("volUuid", volumeUuid)
                .find();

        if(state == PrimaryStorageState.Maintenance){
            throw new OperationFailureException(
                    operr("cannot attach volume[uuid:%s] whose primary storage is Maintenance", volumeUuid));
        }
    }

    protected ImageCacheVO createTemporaryImageCacheFromVolumeSnapshot(ImageInventory image, VolumeSnapshotInventory volumeSnapshot) {
        ImageCacheVO cvo = new ImageCacheVO();
        cvo.setMd5sum("not calculated");

        cvo.setSize(volumeSnapshot.getSize());
        cvo.setInstallUrl(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA + volumeSnapshot.getUuid());
        cvo.setImageUuid(image.getUuid());
        cvo.setPrimaryStorageUuid(self.getUuid());
        cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
        cvo.setState(ImageCacheState.creating);

        logger.debug(String.format("mark volume snapshot[uuid:%s] as temporary image cache", volumeSnapshot.getUuid()));
        return cvo;
    }

    private static String getDeduplicateError(String operationName) {
        return String.format("an other %s task is running, cancel this operation", operationName);
    }

    private void handle(APIScanVmInstanceMetadataFromPrimaryStorageMsg msg) {
        APIScanVmInstanceMetadataFromPrimaryStorageReply reply = new APIScanVmInstanceMetadataFromPrimaryStorageReply();

        if (self.getStatus() != PrimaryStorageStatus.Connected) {
            reply.setError(Platform.operr("primary storage[uuid:%s] is not Connected (status=%s), cannot scan metadata",
                    self.getUuid(), self.getStatus()));
            bus.reply(msg, reply);
            return;
        }

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).findValue();
        if (psType == null) {
            reply.setError(Platform.operr("primary storage[uuid:%s] not found", msg.getPrimaryStorageUuid()));
            bus.reply(msg, reply);
            return;
        }
        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            reply.setError(Platform.operr("primary storage type %s does not support metadata", psType));
            bus.reply(msg, reply);
            return;
        }
        String metadataDir = ext.buildMetadataDir(msg.getPrimaryStorageUuid());

        ScanVmInstanceMetadataFromPrimaryStorageMsg gmsg = new ScanVmInstanceMetadataFromPrimaryStorageMsg();
        gmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        gmsg.setMetadataDir(metadataDir);
        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    bus.reply(msg, reply);
                    return;
                }
                ScanVmInstanceMetadataFromPrimaryStorageReply re = r.castReply();
                List<VmMetadataScanEntry> metadata = re.getVmInstanceMetadata();
                if (metadata == null) {
                    metadata = Collections.emptyList();
                }
                List<VmMetadataScanEntry> filtered = metadata.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> e.getVmCategory() != null)
                        .filter(e -> !VmMetadataCategory.VM_TEMPLATE_CACHE.name().equals(e.getVmCategory()))
                        .collect(Collectors.toList());
                enrichVmMetadataRegistrationHints(filtered, msg.getPrimaryStorageUuid());
                reply.setVmInstanceMetadata(filtered);
                bus.reply(msg, reply);
            }
        });
    }

    private void enrichVmMetadataRegistrationHints(List<VmMetadataScanEntry> metadata, String scannedPrimaryStorageUuid) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        Set<String> vmUuids = metadata.stream()
                .map(VmMetadataScanEntry::getVmUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (vmUuids.isEmpty()) {
            return;
        }

        Map<String, String> existingVmRootPrimaryStorageUuids = new HashMap<>();
        List<Tuple> tuples = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.primaryStorageUuid)
                .in(VolumeVO_.vmInstanceUuid, vmUuids)
                .eq(VolumeVO_.type, VolumeType.Root)
                .listTuple();
        for (Tuple tuple : tuples) {
            String vmUuid = tuple.get(0, String.class);
            String primaryStorageUuid = tuple.get(1, String.class);
            if (vmUuid != null && primaryStorageUuid != null) {
                existingVmRootPrimaryStorageUuids.put(vmUuid, primaryStorageUuid);
            }
        }

        if (existingVmRootPrimaryStorageUuids.isEmpty()) {
            return;
        }

        for (VmMetadataScanEntry entry : metadata) {
            String existingPrimaryStorageUuid = existingVmRootPrimaryStorageUuids.get(entry.getVmUuid());
            if (existingPrimaryStorageUuid == null) {
                continue;
            }

            entry.setRegenerateUuidRequired(entry.isRegenerateUuidRequired()
                    || !Objects.equals(existingPrimaryStorageUuid, scannedPrimaryStorageUuid));
        }
    }
}
