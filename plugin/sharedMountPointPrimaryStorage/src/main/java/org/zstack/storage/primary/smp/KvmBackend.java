package org.zstack.storage.primary.smp;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.ImageBackupStorageSelector;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.core.*;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.*;
import org.zstack.storage.primary.*;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import java.io.File;
import java.util.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/3/26.
 */
public class KvmBackend extends HypervisorBackend {
    private static final CLogger logger = Utils.getLogger(KvmBackend.class);

    @Autowired
    protected ApiTimeoutManager timeoutManager;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected SMPPrimaryStorageFactory primaryStorageFactory;

    public KvmBackend() {
    }

    public static class AgentCmd extends KVMAgentCommands.PrimaryStorageCommand {
        public String mountPoint;
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
        protected ErrorCode buildErrorCode() {
            if (success) {
                return null;
            }
            return operr("operation error, because:%s", error);
        }
    }

    public static class ResizeVolumeCmd extends AgentCmd {
        private String installPath;
        private long size;
        private boolean force;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class ResizeVolumeRsp extends AgentRsp {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class DeleteRsp extends AgentRsp {
        public boolean inUse;
        public ErrorCode buildErrorCode() {
            if (inUse) {
                return Platform.err(VolumeErrors.VOLUME_IN_USE, error);
            }
            return super.buildErrorCode();
        }
    }

    public static class ConnectCmd extends AgentCmd {
        public String uuid;
        public List<String> existUuids;
    }

    public static class ConnectRsp extends AgentRsp {
        public boolean isFirst = false;
    }

    public static class CreateVolumeFromCacheCmd extends AgentCmd {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
        public long virtualSize;
    }

    public static class CreateVolumeFromCacheRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static class CreateVolumeWithBackingCmd extends AgentCmd {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
    }

    public static class CreateVolumeWithBackingRsp extends AgentRsp {
        public long actualSize;
        public long size;
    }

    public static class CreateFolderCmd extends AgentCmd {
        public String installPath;
    }

    public static class DeleteBitsCmd extends AgentCmd {
        public String path;
        public boolean folder = false;
    }

    public static class GetSubPathCmd extends AgentCmd {
        public String path;
    }

    public static class GetSubPathRsp extends AgentRsp {
        public List<String> paths;
    }

    public static class CreateTemplateFromVolumeCmd extends AgentCmd implements HasThreadContext {
        public String installPath;
        public String volumePath;
    }

    public static class CreateTemplateFromVolumeRsp extends AgentRsp {
        public long actualSize;
        public long size;
    }

    public static class EstimateTemplateSizeCmd extends AgentCmd {
        public String volumePath;
    }

    public static class EstimateTemplateSizeRsp extends AgentRsp {
        public long actualSize;
        public long size;
    }

    public static class SftpUploadBitsCmd extends AgentCmd implements HasThreadContext{
        public String primaryStorageInstallPath;
        public String backupStorageInstallPath;
        public String hostname;
        public String username;
        public String sshKey;
        public int sshPort;
    }

    public static class SftpDownloadBitsCmd extends AgentCmd {
        public String sshKey;
        public int sshPort;
        public String hostname;
        public String username;
        public String backupStorageInstallPath;
        public String primaryStorageInstallPath;
    }

    public static class ReInitImageCmd extends AgentCmd {
        public String volumeInstallPath;
        public String imageInstallPath;
    }

    public static class RevertVolumeFromSnapshotCmd extends AgentCmd {
        public String snapshotInstallPath;
    }

    public static class ReInitImageRsp extends AgentRsp {
        @Validation
        public String newVolumeInstallPath;
    }

    public static class RevertVolumeFromSnapshotRsp extends AgentRsp {
        @Validation
        public String newVolumeInstallPath;

        @Validation
        protected long size;
    }

    public static class MergeSnapshotCmd extends AgentCmd implements HasThreadContext {
        public String volumeUuid;
        public String snapshotInstallPath;
        public String workspaceInstallPath;
    }

    public static class MergeSnapshotRsp extends AgentRsp {
        public long actualSize;
        public long size;
    }

    public static class GetDownloadBitsFromKVMHostProgressCmd extends AgentCmd {
        public List<String> volumePaths;
    }

    public static class GetDownloadBitsFromKVMHostProgressRsp extends AgentRsp {
        public long totalSize;
    }

    public static class OfflineMergeSnapshotCmd extends AgentCmd implements HasThreadContext {
        public String srcPath;
        public String destPath;
        public boolean fullRebase;
    }

    public static class OfflineMergeSnapshotRsp extends AgentRsp {
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class OfflineCommitSnapshotCmd extends AgentCmd implements HasThreadContext {
        public String top;
        public String base;
        public List<String> topChildrenInstallPathInDb = new ArrayList<>();
    }

    public static class OfflineCommitSnapshotRsp extends AgentRsp {
        private Long actualSize;

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class CreateEmptyVolumeCmd extends AgentCmd {
        public String installPath;
        public long size;
        public String name;
        public String volumeUuid;
        public String backingFile;
        public String volumeFormat = VolumeConstant.VOLUME_FORMAT_QCOW2;
    }

    public static class CreateEmptyVolumeRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static class CheckBitsCmd extends AgentCmd {
        public String path;
    }

    public static class CheckBitsRsp extends AgentRsp {
        public boolean existing;
    }

    public static class GetVolumeSizeCmd extends AgentCmd {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static class DownloadBitsFromKVMHostRsp extends AgentRsp {
        public String format;
    }

    public static class DownloadBitsFromKVMHostCmd extends AgentCmd {
        public String hostname;
        public String username;
        public String sshKey;
        public int sshPort;
        // it's file path on kvm host actually
        public String backupStorageInstallPath;
        public String primaryStorageInstallPath;
        public Long bandWidth;
        public String identificationCode;
    }

    public static class CancelDownloadBitsFromKVMHostCmd extends AgentCmd {
        public String primaryStorageInstallPath;
    }

    public static class LinkVolumeNewDirCmd extends AgentCmd {
        public String volumeUuid;
        public String srcDir;
        public String dstDir;
    }

    public static class LinkVolumeNewDirRsp extends AgentRsp {
    }

    public static class UnlinkBitsCmd extends AgentCmd {
        public String installPath;
        public boolean onlyLinkedFile = true;
    }

    public static class UnlinkBitsRsp extends AgentRsp {
    }

    public static class GetQcow2HashValueCmd extends AgentCmd {
        private String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class GetQcow2HashValueRsp extends AgentRsp {
        private String hashValue;

        public String getHashValue() {
            return hashValue;
        }

        public void setHashValue(String hashValue) {
            this.hashValue = hashValue;
        }
    }

    public static class GetBackingChainCmd extends AgentCmd {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetBackingChainRsp extends AgentRsp {
        public List<String> backingChain;
        public long totalSize;
    }

    public static class GetBackingFileCmd extends AgentCmd {
        public String volumeUuid;
        public String installPath;
    }

    public static final String CONNECT_PATH = "/sharedmountpointprimarystorage/connect";
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/sharedmountpointprimarystorage/createrootvolume";
    public static final String CREATE_VOLUME_WITH_BACKING_PATH = "/sharedmountpointprimarystorage/createvolumewithbacking";
    public static final String DELETE_BITS_PATH = "/sharedmountpointprimarystorage/bits/delete";
    public static final String UNLINK_BITS_PATH = "/sharedmountpointprimarystorage/bits/unlink";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/sharedmountpointprimarystorage/createtemplatefromvolume";
    public static final String ESTIMATE_TEMPLATE_SIZE_PATH = "/sharedmountpointprimarystorage/estimatetemplatesize";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/volume/revertfromsnapshot";
    public static final String REINIT_IMAGE_PATH = "/sharedmountpointprimarystorage/volume/reinitimage";
    public static final String MERGE_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/snapshot/merge";
    public static final String OFFLINE_MERGE_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/snapshot/offlinemerge";
    public static final String OFFLINE_COMMIT_SNAPSHOT_PATH = "/sharedmountpointprimarystorage/snapshot/offlinecommit";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/sharedmountpointprimarystorage/volume/createempty";
    public static final String CREATE_FOLDER_PATH = "/sharedmountpointprimarystorage/volume/createfolder";
    public static final String CHECK_BITS_PATH = "/sharedmountpointprimarystorage/bits/check";
    public static final String GET_VOLUME_SIZE_PATH = "/sharedmountpointprimarystorage/volume/getsize";
    public static final String HARD_LINK_VOLUME = "/sharedmountpointprimarystorage/volume/hardlink";
    public static final String DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/sharedmountpointprimarystorage/kvmhost/download";
    public static final String CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/sharedmountpointprimarystorage/kvmhost/download/cancel";
    public static final String GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH = "/sharedmountpointprimarystorage/kvmhost/download/progress";
    public static final String GET_QCOW2_HASH_VALUE_PATH = "/sharedmountpointprimarystorage/getqcow2hash";
    public static final String GET_BACKING_CHAIN_PATH = "/sharedmountpointprimarystorage/volume/getbackingchain";
    public static final String RESIZE_VOLUME_PATH = "/sharedmountpointprimarystorage/volume/resize";

    public KvmBackend(PrimaryStorageVO self) {
        super(self);
    }

    private List<String> findConnectedHostByClusterUuid(String clusterUuid, boolean exceptionOnNotFound) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        List<String> hostUuids = q.listValue();
        if (hostUuids.isEmpty() && exceptionOnNotFound) {
            throw new OperationFailureException(operr("no connected host found in the cluster[uuid:%s]", clusterUuid));
        }

        return hostUuids;
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        cmd.mountPoint = self.getMountPath();
        cmd.primaryStorageUuid = self.getUuid();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                ErrorCode errorCode = rsp.buildErrorCode();
                if (errorCode != null) {
                    completion.fail(errorCode);
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(self.getUuid()).run(new PrimaryStorageCapacityUpdaterRunnable() {
                        @Override
                        public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                                cap.setAvailableCapacity(rsp.availableCapacity);
                            }

                            cap.setTotalCapacity(rsp.totalCapacity);
                            cap.setTotalPhysicalCapacity(rsp.totalCapacity);
                            cap.setAvailablePhysicalCapacity(rsp.availableCapacity);

                            return cap;
                        }
                    });
                }

                completion.success(rsp);
            }
        });
    }

    private void connect(String hostUuid, final ReturnValueCompletion<Boolean> completion) {
        ConnectCmd cmd = new ConnectCmd();
        cmd.uuid = self.getUuid();
        cmd.mountPoint = self.getMountPath();
        cmd.existUuids = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, SMPConstants.SMP_TYPE)
                .listValues();

        httpCall(CONNECT_PATH, hostUuid, cmd, true, ConnectRsp.class, new ReturnValueCompletion<ConnectRsp>(completion) {
            @Override
            public void success(ConnectRsp rsp) {
                completion.success(rsp.isFirst);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void attachHook(final String clusterUuid, final Completion completion) {
        connectByClusterUuid(clusterUuid, new ReturnValueCompletion<ClusterConnectionStatus>(completion) {
            @Override
            public void success(ClusterConnectionStatus clusterStatus) {
                if (clusterStatus == ClusterConnectionStatus.PartiallyConnected || clusterStatus == ClusterConnectionStatus.FullyConnected){
                    changeStatus(PrimaryStorageStatus.Connected);
                } else if (self.getStatus() == PrimaryStorageStatus.Disconnected && clusterStatus == ClusterConnectionStatus.Disconnected){
                    hookToKVMHostConnectedEventToChangeStatusToConnected();
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        if (msg instanceof InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createTemporaryRootVolume((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) msg, completion);
        } else if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg, completion);
        } else if (msg instanceof InstantiateTemporaryVolumeOnPrimaryStorageMsg) {
            createTemporaryEmptyVolume((InstantiateTemporaryVolumeOnPrimaryStorageMsg) msg, completion);
        } else if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            createMemoryVolume((InstantiateMemoryVolumeOnPrimaryStorageMsg) msg, completion);
        } else {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
        }
    }

    @Override
    void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion) {
        DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();
        downloadImageToCache(msg.getTemplateSpec(), new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory cache) {
                reply.setImageCache(cache);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    public String makeTemporaryRootVolumeInstallUrl(VolumeInventory vol, String originVolumeUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeTemporaryRootVolumeInstallPath(vol, originVolumeUuid));
    }

    public String makeRootVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }

    public String makeTemporaryDataVolumeInstallUrl(String volUuid, String originVolumeUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeTemporaryDataVolumeInstallPath(volUuid, originVolumeUuid));
    }

    public String makeDataVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public String makeMemoryVolumeParentInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeMemoryVolumeParentInstallPath(vol));
    }

    public String makeMemoryVolumeInstallUrl(VolumeInventory vol) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeMemoryVolumeInstallPath(vol));
    }

    public String makeCachedImageInstallUrl(ImageInventory iminv) {
        return ImageCacheUtil.getImageCachePath(iminv, it -> PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv)));
    }

    public String makeNvRamVolumeInstallUrl(String volUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeNvRamVolumeInstallPath(volUuid));
    }

    public String makeCachedImageInstallUrlFromImageUuidForTemplate(String imageUuid) {
        return PathUtil.join(self.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(imageUuid));
    }

    public String makeTemplateFromVolumeInWorkspacePath(String imageUuid) {
        return PathUtil.join(self.getMountPath(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    public String makeVolumeInstallDir(VolumeInventory vol) {
        String volPath = null;
        if (VolumeType.Data.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(vol.getUuid());
        } else if (VolumeType.Root.toString().equals(vol.getType())) {
            volPath = makeRootVolumeInstallUrl(vol);
        } else if (VolumeType.Memory.toString().equals(vol.getType())) {
            volPath = makeMemoryVolumeInstallUrl(vol);
        } else if (VolumeType.Cache.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(vol.getUuid());
        }

        DebugUtils.Assert(!StringUtils.isEmpty(volPath), "volPath should not be null");
        return new File(volPath).getParentFile().getAbsolutePath();
    }

    public String makeSnapshotInstallPath(VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        String volDir = makeVolumeInstallDir(vol);
        return PathUtil.join(volDir, "snapshots", String.format("%s.qcow2", snapshot.getUuid()));
    }

    public String makeSnapshotInstallPath(VolumeInventory vol, String snapshotUuid) {
        String volDir = makeVolumeInstallDir(vol);
        return PathUtil.join(volDir, "snapshots", String.format("%s.qcow2", snapshotUuid));
    }

    public String makeSnapshotWorkspacePath(String imageUuid) {
        return PathUtil.join(
                self.getMountPath(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }

    class ImageCache {
        ImageInventory image;
        String backupStorageUuid;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
        String volumeResourceInstallPath;

        void download(final ReturnValueCompletion<ImageCacheInventory> completion) {
            DebugUtils.Assert(image != null, "image cannot be null");
            DebugUtils.Assert(primaryStorageInstallPath != null, "primaryStorageInstallPath cannot be null");

            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("download-image-%s-to-shared-mountpoint-storage-%s-cache", image.getUuid(), self.getUuid());
                }

                private void doDownload(final SyncTaskChain chain) {
                    if (volumeResourceInstallPath == null) {
                        DebugUtils.Assert(backupStorageUuid != null, "backup storage UUID cannot be null");
                        DebugUtils.Assert(backupStorageInstallPath != null, "backupStorageInstallPath cannot be null");
                    }

                    FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                    fchain.setName(String.format("download-image-%s-to-shared-mountpoint-storage-%s-cache",
                            image.getUuid(), self.getUuid()));
                    fchain.then(new ShareFlow() {
                        long actualSize = image.getActualSize();
                        ImageCacheVO vo = new ImageCacheVO();

                        @Override
                        public void setup() {
                            flow(new Flow() {
                                String __name__ = "allocate-primary-storage";

                                boolean s = false;

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                                    amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                                    amsg.setSize(image.getActualSize());
                                    amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                                    amsg.setNoOverProvisioning(true);
                                    bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                    bus.send(amsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (reply.isSuccess()) {
                                                s = true;
                                                trigger.next();
                                            } else {
                                                trigger.fail(reply.getError());
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void rollback(FlowRollback trigger, Map data) {
                                    if (s) {
                                        IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                        imsg.setDiskSize(image.getActualSize());
                                        imsg.setNoOverProvisioning(true);
                                        imsg.setPrimaryStorageUuid(self.getUuid());
                                        bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                                        bus.send(imsg);
                                    }

                                    trigger.rollback();
                                }
                            });

                            flow(new NoRollbackFlow() {
                                String __name__ = "download";

                                @Override
                                public void run(final FlowTrigger trigger, Map data) {
                                    if (volumeResourceInstallPath != null) {
                                        downloadFromVolumeResource(trigger);
                                    } else {
                                        downloadFromBackupStorage(trigger);
                                    }
                                }

                                private void downloadFromVolumeResource(FlowTrigger trigger) {
                                    CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                                    cmd.volumePath = volumeResourceInstallPath;
                                    cmd.installPath = primaryStorageInstallPath;
                                    new Do().go(CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd, CreateTemplateFromVolumeRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                                        @Override
                                        public void success(AgentRsp returnValue) {
                                            CreateTemplateFromVolumeRsp rsp = (CreateTemplateFromVolumeRsp) returnValue;
                                            actualSize = rsp.actualSize;
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    });
                                }

                                private void downloadFromBackupStorage(FlowTrigger trigger) {
                                    BackupStorageKvmDownloader downloader = getBackupStorageKvmDownloader(backupStorageUuid);
                                    downloader.downloadBits(backupStorageInstallPath, primaryStorageInstallPath, false, new Completion(trigger) {
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
                                String __name__ = "save-db";

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    vo.setState(ImageCacheState.ready);
                                    vo.setMediaType(ImageMediaType.valueOf(image.getMediaType()));
                                    vo.setImageUuid(image.getUuid());
                                    vo.setPrimaryStorageUuid(self.getUuid());
                                    vo.setSize(actualSize);
                                    vo.setMd5sum("not calculated");
                                    vo.setInstallUrl(primaryStorageInstallPath);
                                    dbf.persist(vo);
                                    logger.debug(String.format("downloaded image[uuid:%s, name:%s] to the image cache of local shared mount point storage[uuid: %s, installPath: %s]",
                                            image.getUuid(), image.getName(), self.getUuid(), primaryStorageInstallPath));
                                    trigger.next();
                                }
                            });

                            flow(new NoRollbackFlow() {
                                String __name__ = "invoke-after-create-image-cache-extensions";
                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    ImageCacheInventory inventory = ImageCacheInventory.valueOf(vo);
                                    new While<>(pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class)).each((ext, whileCompletion) -> {
                                        ext.saveEncryptAfterCreateImageCache(null, inventory, new Completion(whileCompletion) {
                                            @Override
                                            public void success() {
                                                whileCompletion.done();
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                whileCompletion.addError(errorCode);
                                                whileCompletion.done();
                                            }
                                        });
                                    }).run(new WhileDoneCompletion(trigger) {
                                        @Override
                                        public void done(ErrorCodeList errorCodeList) {
                                            if (!errorCodeList.getCauses().isEmpty()) {
                                                String details = multiErr(errorCodeList.getCauses()).getReadableDetails();
                                                logger.warn(String.format(
                                                        "failed to invoke after create image cache extensions (but still continue): %s",
                                                        details));
                                            }
                                            trigger.next();
                                        }
                                    });
                                }
                            });

                            done(new FlowDoneHandler(completion, chain) {
                                @Override
                                public void handle(Map data) {
                                    completion.success(ImageCacheInventory.valueOf(vo));
                                    chain.next();
                                }
                            });

                            error(new FlowErrorHandler(completion, chain) {
                                @Override
                                public void handle(ErrorCode errCode, Map data) {
                                    completion.fail(errCode);
                                    chain.next();
                                }
                            });
                        }
                    }).start();
                }

                private void checkEncryptImageCache(ImageCacheInventory inventory, final SyncTaskChain chain) {
                    List<AfterCreateImageCacheExtensionPoint> extensionList = pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class);

                    if (extensionList.isEmpty()) {
                        completion.success(inventory);
                        chain.next();
                        return;
                    }

                    extensionList.forEach(ext -> ext.checkEncryptImageCache(null, inventory, new Completion(chain) {
                        @Override
                        public void success() {
                            completion.success(inventory);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    }));
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                    q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                    q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                    ImageCacheVO cache = q.find();
                    if (cache == null) {
                        doDownload(chain);
                        return;
                    }

                    CheckBitsCmd cmd = new CheckBitsCmd();
                    cmd.path = primaryStorageInstallPath;

                    new Do().go(CHECK_BITS_PATH, cmd, CheckBitsRsp.class, new ReturnValueCompletion<AgentRsp>(completion, chain) {
                        @Override
                        public void success(AgentRsp returnValue) {
                            CheckBitsRsp rsp = (CheckBitsRsp) returnValue;
                            if (rsp.existing) {
                                checkEncryptImageCache(ImageCacheInventory.valueOf(cache), chain);
                                return;
                            }

                            // the image is removed on the host
                            // delete the cache object and re-download it
                            SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                            q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                            q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                            ImageCacheVO vo = q.find();

                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setDiskSize(vo.getSize());
                            imsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
                            bus.send(imsg);

                            dbf.remove(vo);
                            doDownload(chain);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private void createMemoryVolume(InstantiateMemoryVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final CreateFolderCmd cmd = new CreateFolderCmd();
        cmd.installPath = makeMemoryVolumeParentInstallUrl(msg.getVolume());
        new Do(msg.getDestHost().getUuid()).go(CREATE_FOLDER_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                msg.getVolume().setInstallPath(cmd.installPath);
                reply.setVolume(msg.getVolume());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createTemporaryEmptyVolume(InstantiateTemporaryVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final VolumeInventory volume = msg.getVolume();
        if (VolumeType.Root.toString().equals(volume.getType())) {
            volume.setInstallPath(makeTemporaryRootVolumeInstallUrl(volume, msg.getOriginVolumeUuid()));
        } else {
            volume.setInstallPath(makeTemporaryDataVolumeInstallUrl(volume.getUuid(), msg.getOriginVolumeUuid()));
        }
        volume.setInstallPath(makeTemporaryRootVolumeInstallUrl(volume, msg.getOriginVolumeUuid()));
        createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
    }

    private void createEmptyVolume(final VolumeInventory volume, String hostUuid, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();

        if (StringUtils.isNotEmpty(volume.getInstallPath())) {
            cmd.installPath = volume.getInstallPath();
        } else if (VolumeType.Root.toString().equals(volume.getType())) {
            cmd.installPath = makeRootVolumeInstallUrl(volume);
        } else if (VolumeType.Data.toString().equals(volume.getType())) {
            cmd.installPath = makeDataVolumeInstallUrl(volume.getUuid());
        } else {
            DebugUtils.Assert(false, "Should not be here");
        }
        cmd.name = volume.getName();
        cmd.size = volume.getSize();
        cmd.volumeUuid = volume.getUuid();

        new Do(hostUuid).go(CREATE_EMPTY_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                CreateEmptyVolumeRsp rsp = (CreateEmptyVolumeRsp) returnValue;
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                volume.setInstallPath(cmd.installPath);
                volume.setFormat(cmd.volumeFormat);
                volume.setActualSize(rsp.actualSize);
                if (rsp.size != null) {
                    volume.setSize(rsp.size);
                }

                reply.setVolume(volume);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createTemporaryRootVolume(InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final VolumeInventory volume = msg.getVolume();
        volume.setInstallPath(makeTemporaryRootVolumeInstallUrl(volume, msg.getOriginVolumeUuid()));
        createRootVolume(msg, completion);
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion) {
        final ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg.getVolume(), msg.getDestHost().getUuid(), completion);
            return;
        }

        final VolumeInventory volume = msg.getVolume();
        final String hostUuid = msg.getDestHost().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("kvm-smp-storage-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache = makeCachedImageInstallUrl(image);
            String installPath = StringUtils.isNotEmpty(volume.getInstallPath()) ? volume.getInstallPath() :
                    makeRootVolumeInstallUrl(volume) ;
            Long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
                        dmsg.setTemplateSpec(msg.getTemplateSpec());
                        dmsg.setHostUuid(msg.getDestHost().getUuid());
                        dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                pathInCache = (( DownloadVolumeTemplateToPrimaryStorageReply) reply).getImageCache().getInstallUrl();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeFromCacheCmd cmd = new CreateVolumeFromCacheCmd();
                        cmd.installPath = installPath;
                        cmd.templatePathInCache = makeCachedImageInstallUrl(image);
                        cmd.volumeUuid = volume.getUuid();
                        if (image.getSize() < volume.getSize()) {
                            cmd.virtualSize = volume.getSize();
                        }

                        httpCall(CREATE_VOLUME_FROM_CACHE_PATH, hostUuid, cmd, CreateVolumeFromCacheRsp.class,
                                new ReturnValueCompletion<CreateVolumeFromCacheRsp>(trigger) {
                            @Override
                            public void success(CreateVolumeFromCacheRsp rsp) {
                                actualSize = rsp.actualSize;
                                if (rsp.size != null) {
                                    volume.setSize(rsp.size);
                                }

                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        volume.setInstallPath(installPath);
                        volume.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
                        volume.setActualSize(actualSize);
                        reply.setVolume(volume);
                        completion.success(reply);
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

    class Do {
        private List<String> hostUuids;
        private List<ErrorCode> errors = new ArrayList<ErrorCode>();

        public Do(String huuid) {
            hostUuids = new ArrayList<String>();
            hostUuids.add(huuid);
        }

        public Do() {
            hostUuids = new ArrayList<>();
            List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory(),0,50);
            hinvs.forEach(it -> hostUuids.add(it.getUuid()));
            if (hostUuids.isEmpty()) {
                throw new OperationFailureException(operr("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                                " in the clusters attached with the shared mount point storage[uuid:%s] are disconnected",
                        self.getUuid()));
            }
        }

        void go(String path, AgentCmd cmd, ReturnValueCompletion<AgentRsp> completion) {
            go(path, cmd, AgentRsp.class, completion);
        }

        void go(String path, AgentCmd cmd, Class rspType, ReturnValueCompletion<AgentRsp> completion) {
            doCommand(hostUuids.iterator(), path, cmd, rspType, completion);
        }

        private void doCommand(final Iterator<String> it, final String path, final AgentCmd cmd, final Class rspType, final ReturnValueCompletion<AgentRsp> completion) {
            if (!it.hasNext()) {
                completion.fail(errf.stringToOperationError("an operation failed on all hosts", errors));
                return;
            }

            final String hostUuid = it.next();
            httpCall(path, hostUuid, cmd, rspType, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp rsp) {
                    completion.success(rsp);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE) || errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                        completion.fail(errorCode);
                        return;
                    }

                    errors.add(errorCode);
                    logger.warn(String.format("failed to do the command[%s] on the kvm host[uuid:%s], %s, try next one",
                            cmd.getClass(), hostUuid, errorCode));
                    doCommand(it, path, cmd, rspType, completion);
                }
            });
        }
    }


    @Override
    void handle(DeleteVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion) {
        boolean dir = msg.getVolume().getType().equals(VolumeType.Memory.toString());

        deleteBits(msg.getVolume().getInstallPath(), dir, new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }


    @Override
    void handle(final DownloadDataVolumeToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion) {
        String installPath;
        if (msg instanceof DownloadTemporaryDataVolumeToPrimaryStorageMsg) {
            String originVolumeUuid = ((DownloadTemporaryDataVolumeToPrimaryStorageMsg) msg).getOriginVolumeUuid();
            installPath = makeTemporaryDataVolumeInstallUrl(msg.getVolumeUuid(), originVolumeUuid);
        } else {
            installPath = makeDataVolumeInstallUrl(msg.getVolumeUuid());
        }
        BackupStorageKvmDownloader downloader = getBackupStorageKvmDownloader(msg.getBackupStorageRef().getBackupStorageUuid());
        downloader.downloadBits(msg.getBackupStorageRef().getInstallPath(), installPath, true, new Completion(completion) {
            @Override
            public void success() {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setFormat(msg.getImage().getFormat());
                reply.setInstallPath(installPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion) {
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        if (msg instanceof GetInstallPathForTemporaryDataVolumeDownloadMsg) {
            String originVolumeUuid = ((GetInstallPathForTemporaryDataVolumeDownloadMsg) msg).getOriginVolumeUuid();
            reply.setInstallPath(makeTemporaryDataVolumeInstallUrl(msg.getVolumeUuid(), originVolumeUuid));
        } else {
            reply.setInstallPath(makeDataVolumeInstallUrl(msg.getVolumeUuid()));
        }
        completion.success(reply);
    }

    @Override
    void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion) {
        deleteBits(msg.getInstallPath(), msg.isFolder(), new Completion(completion) {
            @Override
            public void success() {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(final DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion) {
        deleteBits(msg.getInstallPath(), msg.isFolder(), new Completion(completion) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DownloadIsoToPrimaryStorageMsg msg, final ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion) {
        downloadImageToCache(msg.getIsoSpec(), new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setInstallPath(returnValue.getInstallUrl());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion) {
        // The ISO is in the image cache, no need to delete it
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        completion.success(reply);
    }

    @Override
    void handle(CheckSnapshotMsg msg, Completion completion) {
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));

        String hostUuid;
        String connectedHostUuid = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null){
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused){
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                hostUuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped){
                hostUuid = connectedHostUuid;
            } else {
                completion.fail(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state[%s]",
                        vol.getVmInstanceUuid(), state));
                return;
            }
        } else {
            hostUuid = connectedHostUuid;
        }

        CheckSnapshotOnHypervisorMsg hmsg = new CheckSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(hostUuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolumeUuid(vol.getUuid());
        hmsg.setVolumeChainToCheck(msg.getVolumeChainToCheck());
        hmsg.setCurrentInstallPath(vol.getInstallPath());
        hmsg.setPrimaryStorageUuid(self.getUuid());
        if (vol.getRootImageUuid() != null) {
            String installUrl = getImageCacheInstallPath(vol.getRootImageUuid());
            if (installUrl != null) {
                hmsg.getExcludeInstallPaths().add(installUrl);
            }
        }
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(hmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply ret) {
                if (!ret.isSuccess()) {
                    completion.fail(ret.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    private String getImageCacheInstallPath(String imageUuid) {
        return Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.installUrl)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, imageUuid).findValue();
    }

    @Override
    void handle(TakeSnapshotMsg msg, final ReturnValueCompletion<TakeSnapshotReply> completion) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));

        String hostUuid;
        String connectedHostUuid = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        if (vol.getVmInstanceUuid() != null){
            Tuple t = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                    .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid())
                    .findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            String vmHostUuid = t.get(1, String.class);

            if (state == VmInstanceState.Running || state == VmInstanceState.Paused){
                DebugUtils.Assert(vmHostUuid != null,
                        String.format("vm[uuid:%s] is Running or Paused, but has no hostUuid", vol.getVmInstanceUuid()));
                hostUuid = vmHostUuid;
            } else if (state == VmInstanceState.Stopped){
                hostUuid = connectedHostUuid;
            } else {
                completion.fail(operr("vm[uuid:%s] is not Running, Paused or Stopped, current state[%s]",
                        vol.getVmInstanceUuid(), state));
                return;
            }
        } else {
            hostUuid = connectedHostUuid;
        }


        TakeSnapshotOnHypervisorMsg hmsg = new TakeSnapshotOnHypervisorMsg();
        hmsg.setHostUuid(hostUuid);
        hmsg.setVmUuid(vol.getVmInstanceUuid());
        hmsg.setVolume(vol);
        hmsg.setSnapshotName(msg.getStruct().getCurrent().getUuid());
        hmsg.setFullSnapshot(msg.getStruct().isFullSnapshot());
        String installPath = makeSnapshotInstallPath(vol, sp);
        hmsg.setInstallPath(installPath);
        bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(hmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                TakeSnapshotOnHypervisorReply treply = (TakeSnapshotOnHypervisorReply) reply;
                sp.setSize(treply.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(treply.getSnapshotInstallPath());
                sp.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());

                TakeSnapshotReply ret = new TakeSnapshotReply();
                ret.setNewVolumeInstallPath(treply.getNewVolumeInstallPath());
                ret.setInventory(sp);

                completion.success(ret);
            }
        });
    }

    @Override
    void handle(DeleteSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion) {
        deleteBits(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {
        VolumeSnapshotInventory sp = msg.getSnapshot();
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.snapshotInstallPath = sp.getPrimaryStorageInstallPath();

        new Do().go(REVERT_VOLUME_FROM_SNAPSHOT_PATH, cmd, RevertVolumeFromSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                RevertVolumeFromSnapshotRsp rsp = (RevertVolumeFromSnapshotRsp) returnValue;
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(rsp.newVolumeInstallPath);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, final ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion) {
        ReInitImageCmd cmd = new ReInitImageCmd();
        cmd.imageInstallPath = makeCachedImageInstallUrlFromImageUuidForTemplate(msg.getVolume().getRootImageUuid());
        cmd.volumeInstallPath = makeRootVolumeInstallUrl(msg.getVolume());

        new Do().go(REINIT_IMAGE_PATH, cmd, ReInitImageRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                ReInitImageRsp rsp = (ReInitImageRsp) returnValue;
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setNewVolumeInstallPath(rsp.newVolumeInstallPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            createIncrementalVolumeFromSnapshot(msg.getSnapshot(), msg.getVolumeUuid(), completion);
        } else {
            createNormalVolumeFromSnapshot(msg.getSnapshot(), msg.getVolumeUuid(), completion);
        }
    }

    private void createNormalVolumeFromSnapshot(VolumeSnapshotInventory latest, String volumeUuid, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(volumeUuid);
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        new Do().go(MERGE_SNAPSHOT_PATH, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                MergeSnapshotRsp rsp = (MergeSnapshotRsp) returnValue;
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setInstallPath(installPath);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createIncrementalVolumeFromSnapshot(VolumeSnapshotInventory latest, String volumeUuid, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String installPath = makeDataVolumeInstallUrl(volumeUuid);
        CreateVolumeWithBackingCmd cmd = new CreateVolumeWithBackingCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.templatePathInCache = latest.getPrimaryStorageInstallPath();
        cmd.installPath = installPath;

        new Do().go(CREATE_VOLUME_WITH_BACKING_PATH, cmd, CreateVolumeWithBackingRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                CreateVolumeWithBackingRsp rsp = (CreateVolumeWithBackingRsp) returnValue;
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setInstallPath(installPath);
                reply.setSize(rsp.size);
                reply.setIncremental(true);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetDownloadBitsFromKVMHostProgressMsg msg, final ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion) {
        GetDownloadBitsFromKVMHostProgressCmd cmd = new GetDownloadBitsFromKVMHostProgressCmd();
        cmd.volumePaths = msg.getVolumePaths();

        new Do().go(GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH, cmd, GetDownloadBitsFromKVMHostProgressRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                GetDownloadBitsFromKVMHostProgressRsp rsp = (GetDownloadBitsFromKVMHostProgressRsp) returnValue;
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setTotalSize(rsp.totalSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, final Completion completion) {
        boolean offline = true;
        VolumeInventory volume = to;
        fullRebase |= from == null;

        if (volume.getType().equals(VolumeType.Memory.toString())) {
            completion.success();
            return;
        }

        String hostUuid = null;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            hostUuid = t.get(1, String.class);

            if (state != VmInstanceState.Stopped && state != VmInstanceState.Running
                    && state != VmInstanceState.Destroyed && state != VmInstanceState.Paused) {
                throw new OperationFailureException(operr("the volume[uuid;%s] is attached to a VM[uuid:%s] which is in state of %s, cannot do the snapshot merge",
                                volume.getUuid(), volume.getVmInstanceUuid(), state));
            }

            offline = (state == VmInstanceState.Stopped || state == VmInstanceState.Destroyed);
        }

        if (offline) {
            OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
            cmd.fullRebase = fullRebase;
            cmd.srcPath =  from != null ? from.getPrimaryStorageInstallPath() : null;
            cmd.destPath = volume.getInstallPath();

            new Do().go(OFFLINE_MERGE_SNAPSHOT_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp returnValue) {
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } else {
            MergeVolumeSnapshotOnKvmMsg kmsg = new MergeVolumeSnapshotOnKvmMsg();
            kmsg.setFullRebase(fullRebase);
            kmsg.setHostUuid(hostUuid);
            kmsg.setFrom(from);
            kmsg.setTo(volume);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(r.getError());
                    }
                }
            });
        }
    }

    @Override
    void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion) {
        GetKVMHostDownloadCredentialMsg gmsg = new GetKVMHostDownloadCredentialMsg();
        gmsg.setHostUuid(msg.getSrcHostUuid());

        if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.hasTag(self.getUuid())) {
            gmsg.setDataNetworkCidr(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByResourceUuid(self.getUuid(), PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN));
        }

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, msg.getSrcHostUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    completion.fail(rly.getError());
                    return;
                }

                GetKVMHostDownloadCredentialReply grly = rly.castReply();
                DownloadBitsFromKVMHostCmd cmd = new DownloadBitsFromKVMHostCmd();
                cmd.hostname = grly.getHostname();
                cmd.username = grly.getUsername();
                cmd.sshKey = grly.getSshKey();
                cmd.sshPort = grly.getSshPort();
                cmd.backupStorageInstallPath = msg.getHostInstallPath();
                cmd.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();
                cmd.bandWidth = msg.getBandWidth();
                cmd.identificationCode = msg.getLongJobUuid() + msg.getPrimaryStorageInstallPath();

                new Do(msg.getDestHostUuid()).go(DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, DownloadBitsFromKVMHostRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
                    @Override
                    public void success(AgentRsp returnValue) {
                        DownloadBitsFromKVMHostRsp rsp = (DownloadBitsFromKVMHostRsp) returnValue;
                        DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                        reply.setFormat(rsp.format);
                        completion.success(reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });

    }

    @Override
    void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, Completion completion) {
        CancelDownloadBitsFromKVMHostCmd cmd = new CancelDownloadBitsFromKVMHostCmd();
        cmd.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();

        new Do(msg.getDestHostUuid()).go(CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void downloadImageToCache(ImageSpec img, final ReturnValueCompletion<ImageCacheInventory> completion) {
        ImageInventory imgInv = img.getInventory();
        String backupStorageInstallPath = null;
        String bsUuid = null;

        if (img.getSelectedBackupStorage() != null) {
            backupStorageInstallPath = img.getSelectedBackupStorage().getInstallPath();
            bsUuid = img.getSelectedBackupStorage().getBackupStorageUuid();
        } else if (img.isNeedDownload()) {
            ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
            selector.setZoneUuid(self.getZoneUuid());
            selector.setImageUuid(imgInv.getUuid());
            bsUuid = selector.select();
            if (bsUuid == null) {
                throw new OperationFailureException(operr(
                        "the image[uuid:%s, name: %s] is not available to download on any backup storage:\n" +
                                "1. check if image is in status of Deleted\n" +
                                "2. check if the backup storage on which the image is shown as Ready is attached to the zone[uuid:%s]",
                        imgInv.getUuid(), imgInv.getName(), self.getZoneUuid()
                ));
            }

            final String finalBsUuid = bsUuid;
            ImageBackupStorageRefInventory ref = CollectionUtils.findOneOrNull(imgInv.getBackupStorageRefs(),
                    arg -> arg.getBackupStorageUuid().equals(finalBsUuid));

            backupStorageInstallPath = ref.getInstallPath();
        }



        final ImageCache cache = new ImageCache();
        cache.image = imgInv;
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(imgInv);
        cache.backupStorageUuid = bsUuid;
        cache.backupStorageInstallPath = backupStorageInstallPath;
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion) {
        String hostUuid = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        GetQcow2HashValueCmd cmd = new GetQcow2HashValueCmd();
        cmd.setInstallPath(msg.getPrimaryStorageInstallPath());
        new KvmCommandSender(hostUuid).send(cmd, GET_QCOW2_HASH_VALUE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetQcow2HashValueRsp rsp = wrapper.getResponse(GetQcow2HashValueRsp.class);
                return rsp.success ? null : operr("operation error, because:%s", rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
                GetQcow2HashValueRsp rsp = returnValue.getResponse(GetQcow2HashValueRsp.class);
                reply.setEncrypt(rsp.hashValue);
                reply.setSnapshotUuid(msg.getSnapshotUuid());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> completion) {
        GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
        new While<>(msg.getRootInstallPaths()).each((installPath, compl) -> {
            GetBackingChainCmd cmd = new GetBackingChainCmd();
            cmd.installPath = installPath;
            cmd.volumeUuid = msg.getVolumeUuid();
            new Do().go(GET_BACKING_CHAIN_PATH, cmd, GetBackingChainRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
                @Override
                public void success(AgentRsp r) {
                    GetBackingChainRsp rsp = (GetBackingChainRsp) r;
                    if (CollectionUtils.isEmpty(rsp.backingChain)) {
                        reply.putBackingChainInstallPath(installPath, Collections.emptyList());
                        reply.putBackingChainSize(installPath, 0L);
                    } else {
                        reply.putBackingChainInstallPath(installPath, rsp.backingChain);
                        reply.putBackingChainSize(installPath, rsp.totalSize);
                    }
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList err) {
                if (!err.getCauses().isEmpty()) {
                    completion.fail(err.getCauses().get(0));
                } else {
                    completion.success(reply);
                }
            }
        });
    }

    void createEmptyVolumeWithBackingFile(final VolumeInventory volume, String hostUuid, String backingFile, final ReturnValueCompletion<KvmBackend.AgentRsp> completion) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = volume.getInstallPath();
        cmd.name = volume.getName();
        cmd.size = volume.getSize();
        cmd.volumeUuid = volume.getUuid();
        cmd.backingFile = backingFile;

        new Do(hostUuid).go(CREATE_EMPTY_VOLUME_PATH, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void deleteBits(String path, final Completion completion) {
        deleteBits(path, false, completion);
    }

    @Override
    void deleteBits(String path, boolean folder, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.path = path;
        cmd.folder = folder;
        new Do().go(DELETE_BITS_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion) {
        CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();

        final ImageCache cache = new ImageCache();
        cache.image = msg.getImageInventory();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(msg.getImageInventory());
        cache.volumeResourceInstallPath = msg.getVolumeInventory().getInstallPath();
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory cache) {
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        boolean useSnapshotAsCache = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
        if (useSnapshotAsCache) {
            ImageCacheVO cache = createTemporaryImageCacheFromVolumeSnapshot(msg.getImageInventory(), msg.getVolumeSnapshot());
            dbf.persist(cache);
            reply.setInventory(cache.toInventory());
            reply.setIncremental(true);
            completion.success(reply);
            return;
        }

        final ImageCache cache = new ImageCache();
        cache.image = msg.getImageInventory();
        cache.primaryStorageInstallPath = makeCachedImageInstallUrl(msg.getImageInventory());
        cache.volumeResourceInstallPath = msg.getVolumeSnapshot().getPrimaryStorageInstallPath();
        cache.download(new ReturnValueCompletion<ImageCacheInventory>(completion) {
            @Override
            public void success(ImageCacheInventory inv) {
                reply.setInventory(inv);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolumeInventory();
        final ImageInventory image = msg.getImageInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-from-volume-%s", image.getUuid(), volume.getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            String temporaryTemplatePath = makeTemplateFromVolumeInWorkspacePath(msg.getImageInventory().getUuid());
            String backupStorageInstallPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-temporary-template";

                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
                        cmd.volumePath = volume.getInstallPath();
                        cmd.installPath = temporaryTemplatePath;
                        new Do().go(CREATE_TEMPLATE_FROM_VOLUME_PATH, cmd, CreateTemplateFromVolumeRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp rsp) {
                                CreateTemplateFromVolumeRsp crsp = (CreateTemplateFromVolumeRsp) rsp;
                                reply.setActualSize(crsp.actualSize);
                                success = true;
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
                        if (success) {
                            deleteBits(temporaryTemplatePath, new Completion(trigger) {
                                @Override
                                public void success() {
                                    // pass
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    //TODO GC
                                    logger.warn(String.format("failed to delete %s, %s", temporaryTemplatePath, errorCode));
                                }
                            });
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-to-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                        bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bmsg.setImageMediaType(image.getMediaType());
                        bmsg.setImageUuid(image.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        MessageReply br = bus.call(bmsg);
                        if (!br.isSuccess()) {
                            trigger.fail(br.getError());
                            return;
                        }

                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
                        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                        uploader.uploadBits(msg.getImageInventory().getUuid(), backupStorageInstallPath, temporaryTemplatePath, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String bsPath) {
                                backupStorageInstallPath = bsPath;
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
                    String __name__ = "delete-temporary-template-on-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteBits(temporaryTemplatePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                // do-nothing
                                // no trigger.next, because the flow is GC
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: GC
                                logger.warn(String.format("failed to delete %s on shared mount point primary storage[uuid: %s], %s; need a cleanup", temporaryTemplatePath, self.getUuid(), errorCode));
                            }
                        });

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setFormat(volume.getFormat());
                        reply.setTemplateBackupStorageInstallPath(backupStorageInstallPath);
                        completion.success(reply);
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

    @Override
    public void handle(UploadBitsToBackupStorageMsg msg, final ReturnValueCompletion<UploadBitsToBackupStorageReply> completion) {
        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
        uploader.uploadBits(msg.getImageUuid(), msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private BackupStorageKvmDownloader getBackupStorageKvmDownloader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        for (BackupStorageKvmFactory f : pluginRgty.getExtensionList(BackupStorageKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createDownloader(getSelfInventory(), backupStorageUuid);
            }
        }
        throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }


    private BackupStorageKvmUploader getBackupStorageKvmUploader(String backupStorageUuid) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, backupStorageUuid);
        String bsType = q.findValue();

        if (bsType == null) {
            throw new OperationFailureException(operr("cannot find backup storage[uuid:%s]", backupStorageUuid));
        }


        for (BackupStorageKvmFactory f : pluginRgty.getExtensionList(BackupStorageKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createUploader(getSelfInventory(), backupStorageUuid);
            }
        }

        throw new CloudRuntimeException(String.format("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }


    @Override
    void handleHypervisorSpecificMessage(SMPPrimaryStorageHypervisorSpecificMessage msg) {
        if (msg instanceof InitKvmHostMsg) {
            stream((InitKvmHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message) msg);
        }
    }

    private void cleanInvalidIdFile(List<String> hostUuids){
        for(String huuid :hostUuids){
            String idFilePath = PathUtil.join(self.getMountPath(), "zstack_smp_id_file", self.getUuid());
            DeleteBitsCmd cmd = new DeleteBitsCmd();
            cmd.path = idFilePath;

            httpCall(DELETE_BITS_PATH, huuid, cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(null) {
                @Override
                public void success(AgentRsp rsp) {}

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("fail to clean invalid id file %s in host[uuid:%s], please delete it", idFilePath, huuid));
                }
            });
        }
    }

    @Override
    void connectByClusterUuid(final String clusterUuid, final ReturnValueCompletion<ClusterConnectionStatus> completion) {
        List<String> huuids = findConnectedHostByClusterUuid(clusterUuid, false);
        if (huuids.isEmpty()) {
            // no host in the cluster
            completion.success(ClusterConnectionStatus.Disconnected);
            return;
        }

        class Result {
            Set<ErrorCode> errorCodes = new HashSet<>();
            List<String> huuids = Collections.synchronizedList(new ArrayList<String>());
            List<String> firstAccessHostUuids = Collections.synchronizedList(new ArrayList<String>());
        }

        final Result ret = new Result();
        final AsyncLatch latch = new AsyncLatch(huuids.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                if(ret.firstAccessHostUuids.size() > 1){
                    ret.huuids.addAll(ret.firstAccessHostUuids);
                    ret.errorCodes.add(operr(
                            "hosts[uuid:%s] have the same mount path, but actually mount different storage.",
                            ret.firstAccessHostUuids
                    ));
                }

                if (!ret.errorCodes.isEmpty()) {
                    cleanInvalidIdFile(ret.firstAccessHostUuids);

                    String mountPathErrorInfo = "Can't access mount path on ";
                    for(String hostUuid : ret.huuids) {
                        mountPathErrorInfo += String.format("host[uuid:%s] ", hostUuid);
                    }
                    completion.fail(errf.stringToOperationError(
                            String.format("unable to connect the shared mount point storage[uuid:%s, name:%s] to the cluster[uuid:%s], %s",
                                    self.getUuid(), self.getName(), clusterUuid, mountPathErrorInfo),
                            new ArrayList<>(ret.errorCodes)
                    ));
                } else {
                    completion.success(ClusterConnectionStatus.FullyConnected);
                }
            }
        });

        for (String huuid : huuids) {
            connect(huuid, new ReturnValueCompletion<Boolean>(latch) {
                @Override
                public void success(Boolean isFirst) {
                    // If cluster has multi hosts, they have the same mount point
                    // the host first to access mount point will create a ps_uuid_file
                    // if more than one host created ps_uuid_file, it means that hosts mount different storage
                    // isFirst is true when the host is the first one access mount point
                    if(isFirst){
                        ret.firstAccessHostUuids.add(huuid);
                    }
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    ret.errorCodes.add(errorCode);
                    ret.huuids.add(huuid);
                    latch.ack();
                }
            });
        }
    }

    @Override
    void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, final ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion) {
        String hostUuid = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();
        final GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();
        cmd.mountPoint = self.getMountPath();
        cmd.primaryStorageUuid = msg.getPrimaryStorageUuid();
        new KvmCommandSender(hostUuid).send(cmd, GET_VOLUME_SIZE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeSizeRsp rsp = wrapper.getResponse(GetVolumeSizeRsp.class);
                return rsp.success ? null : operr("operation error, because:%s", rsp.error);
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                GetVolumeSizeRsp rsp = returnValue.getResponse(GetVolumeSizeRsp.class);
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    @Override
    void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion) {
        EstimateTemplateSizeCmd cmd = new EstimateTemplateSizeCmd();
        cmd.volumePath = msg.getInstallPath();

        new Do().go(ESTIMATE_TEMPLATE_SIZE_PATH, cmd, EstimateTemplateSizeRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp r) {
                EstimateTemplateSizeRsp rsp = (EstimateTemplateSizeRsp) r;
                EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CreateTemporaryVolumeFromSnapshotMsg msg, final ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        final String installPath = makeTemplateFromVolumeInWorkspacePath(msg.getTemporaryVolumeUuid());
        VolumeSnapshotInventory latest = msg.getSnapshot();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.volumeUuid = latest.getVolumeUuid();
        cmd.snapshotInstallPath = latest.getPrimaryStorageInstallPath();
        cmd.workspaceInstallPath = installPath;

        new Do().go(MERGE_SNAPSHOT_PATH, cmd, MergeSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setInstallPath(installPath);
                MergeSnapshotRsp mrsp = (MergeSnapshotRsp) rsp;
                reply.setSize(mrsp.size);
                reply.setActualSize(mrsp.actualSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                deleteBits(installPath, new NopeCompletion());
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, final ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion) {
        VolumeSnapshotInventory sinv = msg.getSnapshot();
        String bsUuid = msg.getBackupStorage().getUuid();

        // Get the backup storage install path
        BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
        bmsg.setImageMediaType(VolumeSnapshotVO.class.getSimpleName());
        bmsg.setBackupStorageUuid(msg.getBackupStorage().getUuid());
        bmsg.setImageUuid(sinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorage().getUuid());
        MessageReply br = bus.call(bmsg);
        if (!br.isSuccess()) {
            completion.fail(br.getError());
            return;
        }

        final String installPath = ((BackupStorageAskInstallPathReply) br).getInstallPath();
        BackupStorageKvmUploader uploader = getBackupStorageKvmUploader(bsUuid);
        uploader.uploadBits(null, installPath, sinv.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String bsPath) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setBackupStorageInstallPath(bsPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void stream(final InitKvmHostMsg msg) {
        final InitKvmHostReply reply = new InitKvmHostReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                boolean isFirstAccessPS =
                        (long) SQL.New("select count(host) from HostVO host, PrimaryStorageClusterRefVO cref " +
                                "where cref.primaryStorageUuid = :psUuid " +
                                "and host.uuid != :hostUuid " +
                                "and host.status = :status " +
                                "and host.clusterUuid = cref.clusterUuid")
                                .param("psUuid", msg.getPrimaryStorageUuid())
                                .param("hostUuid", msg.getHostUuid())
                                .param("status", HostStatus.Connected)
                                .find() == 0;
                connect(msg.getHostUuid(), new ReturnValueCompletion<Boolean>(msg, chain) {
                    @Override
                    public void success(Boolean isFirst) {
                        if (isFirst && !isFirstAccessPS) {
                            reply.setError(argerr("host[uuid:%s] might mount storage which is different from SMP[uuid:%s], please check it", msg.getHostUuid(), msg.getPrimaryStorageUuid()));
                            cleanInvalidIdFile(Collections.singletonList(msg.getHostUuid()));
                        }
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
                return String.format("check validity of mounting SMP[uuid:%s]",msg.getPrimaryStorageUuid());
            }
        });
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        reply.setSnapshotInstallPath(makeSnapshotInstallPath(msg.getVolumeInventory(), msg.getSnapshotUuid()));
        completion.success(reply);
    }

    @Override
    void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> completion) {
        ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();

        String originType = msg.getVolume().getType();
        LinkVolumeNewDirCmd cmd = new LinkVolumeNewDirCmd();
        cmd.srcDir = makeVolumeInstallDir(msg.getVolume());
        msg.getVolume().setType(msg.getTargetType().toString());
        cmd.dstDir = makeVolumeInstallDir(msg.getVolume());
        msg.getVolume().setType(originType);
        cmd.volumeUuid = msg.getVolume().getUuid();

        if (!msg.getVolume().getInstallPath().startsWith(cmd.srcDir)) {
            completion.fail(operr("why volume[uuid:%s, installPath:%s] not in directory %s",
                    cmd.volumeUuid, msg.getVolume().getInstallPath(), cmd.srcDir));
            return;
        }


        new Do().go(HARD_LINK_VOLUME, cmd, LinkVolumeNewDirRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                VolumeInventory vol = msg.getVolume();
                String newPath = vol.getInstallPath().replace(cmd.srcDir, cmd.dstDir);
                vol.setInstallPath(newPath);
                reply.setVolume(vol);

                for (VolumeSnapshotInventory snapshot : msg.getSnapshots()) {
                    newPath = snapshot.getPrimaryStorageInstallPath().replace(cmd.srcDir, cmd.dstDir);
                    snapshot.setPrimaryStorageInstallPath(newPath);
                }
                reply.getSnapshots().addAll(msg.getSnapshots());
                reply.setInstallPathToGc(cmd.srcDir);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(UnlinkBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply> completion) {
        UnlinkBitsCmd cmd = new UnlinkBitsCmd();
        cmd.installPath = msg.getInstallPath();
        new Do().go(UNLINK_BITS_PATH, cmd, LinkVolumeNewDirRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success(new UnlinkBitsOnPrimaryStorageReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(ResizeVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<ResizeVolumeOnPrimaryStorageReply> completion) {
        VolumeInventory volume = msg.getVolume();
        final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();

        String hostUuid = primaryStorageFactory.getConnectedHostForOperation(getSelfInventory()).get(0).getUuid();

        ResizeVolumeCmd cmd = new ResizeVolumeCmd();
        cmd.setInstallPath(volume.getInstallPath());
        cmd.setSize(msg.getSize());
        cmd.setForce(msg.isForce());
        httpCall(RESIZE_VOLUME_PATH, hostUuid, cmd, ResizeVolumeRsp.class, new ReturnValueCompletion<ResizeVolumeRsp>(msg) {
            @Override
            public void success(ResizeVolumeRsp rsp) {
                volume.setSize(rsp.getSize());
                reply.setVolume(volume);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion) {
        OfflineCommitSnapshotCmd cmd = new OfflineCommitSnapshotCmd();
        cmd.top = msg.getSrcSnapshot().getPrimaryStorageInstallPath();
        cmd.base = msg.getDstSnapshot().getPrimaryStorageInstallPath();
        cmd.topChildrenInstallPathInDb = msg.getSrcChildrenInstallPathInDb();
        new Do().go(OFFLINE_COMMIT_SNAPSHOT_PATH, cmd, OfflineCommitSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                OfflineCommitSnapshotRsp rsp = (OfflineCommitSnapshotRsp) returnValue;

                CommitVolumeSnapshotOnPrimaryStorageReply reply = new CommitVolumeSnapshotOnPrimaryStorageReply();
                reply.setSize(rsp.getActualSize());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion) {
        PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();
        OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
        cmd.srcPath = msg.getSrcSnapshotParentPath();
        cmd.destPath = msg.getDstSnapshot().getPrimaryStorageInstallPath();
        cmd.fullRebase = cmd.srcPath == null;
        new Do().go(OFFLINE_MERGE_SNAPSHOT_PATH, cmd, OfflineMergeSnapshotRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                OfflineMergeSnapshotRsp rsp = (OfflineMergeSnapshotRsp) returnValue;
                reply.setSize(rsp.getActualSize());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
