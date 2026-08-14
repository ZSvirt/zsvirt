package org.zstack.storage.primary.sharedblock;

import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.ShrinkVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.snapshot.ShrinkVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageReply;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageMsg;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;

public abstract class SharedBlockHypervisorBackend extends SharedBlockGroupPrimaryStorageBase {
    public SharedBlockHypervisorBackend() {
    }

    public SharedBlockHypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion);

    abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion);

    abstract void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion);

    abstract void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion);

    abstract void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion);

    abstract void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion);

    abstract void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion);

    abstract void handle(CheckSnapshotMsg msg, Completion completion);

    abstract void handle(TakeSnapshotMsg msg, ReturnValueCompletion<TakeSnapshotReply> completion);

    abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion);

    abstract void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, Completion completion);

    abstract void handle(CreateTemporaryVolumeFromSnapshotMsg msg, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    abstract void handle(UploadBitsToBackupStorageMsg msg, ReturnValueCompletion<UploadBitsToBackupStorageReply> completion);

    abstract void deleteBits(String path, Completion completion);

    abstract void deleteBits(String path, boolean folder, Completion completion);

    abstract void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion);

    abstract void connectByClusterUuid(String clusterUuid, boolean rescan, ReturnValueCompletion<ClusterConnectionStatus> completion);

    abstract void disconnectByClusterUuid(String clusterUuid, Completion completion);

    abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion);

    abstract void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion);

    abstract void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion);

    abstract void downloadImageToCache(ImageInventory img, final ReturnValueCompletion<String> completion);

    abstract void getPhysicalCapacity(PrimaryStorageInventory inv, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handleHypervisorSpecificMessage(SharedBlockGroupPrimaryStorageHypervisorSpecificMessage msg);

    abstract void beforeSnapshotTake(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion);

    abstract void afterSnapshotTake(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp);

    abstract void afterSnapshotTakeFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err);

    abstract void addSharedBlockToSharedBlockGroup(String diskUuid, String sharedBlockGroupUuid, Completion completion);

    abstract void handle(TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg msg, ReturnValueCompletion<TakeSnapshotOnSharedBlockGroupPrimaryStorageReply> completion);

    abstract void handle(MigrateVolumesBetweenSharedBlockGroupPrimaryStorageMsg msg, ReturnValueCompletion<MigrateVolumesBetweenSharedBlockGroupPrimaryStorageReply> completion);

    abstract void handle(APIRefreshSharedblockDeviceCapacityMsg msg, Completion completion);

    abstract void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion);

    abstract void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> completion);

    abstract void handle(ConfigureFilterMsg msg, Completion completion);

    abstract void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<ShrinkVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion);

    abstract void handle(ActivateVolumeOnPrimaryStorageMsg msg, Completion completion);

    abstract void ping(String psUuid, Completion completion);

    abstract void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void beforeBlockCommit(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, Completion completion);

    abstract void afterBlockCommit(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, CommitVolumeSnapshotOnHypervisorReply reply, Completion completion);

    abstract void afterBlockCommitFailed(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, KVMAgentCommands.BlockCommitResponse rsp, ErrorCode err);

    abstract void beforeBlockPull(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, Completion completion);

    abstract void afterBlockPull(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, PullVolumeSnapshotOnHypervisorReply reply, Completion completion);

    abstract void afterBlockPullFailed(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, KVMAgentCommands.BlockPullResponse rsp, ErrorCode err);

    abstract void checkPrimaryStorageConsistency(ReturnValueCompletion<ConsistencyCheckResult> completion);

    abstract void takeover(String hostUuid, ReturnValueCompletion<SharedBlockKvmCommands.TakeoverRsp> completion);

    abstract void handle(UpdateVmInstanceMetadataOnPrimaryStorageMsg msg, ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply> completion);

    abstract void handle(GetVmInstanceMetadataFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply> completion);

    abstract void handle(ScanVmInstanceMetadataFromPrimaryStorageMsg msg, ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply> completion);

    abstract void handle(CleanupVmInstanceMetadataOnPrimaryStorageMsg msg, ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply> completion);

    abstract void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg, ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply> completion);

}
