package org.zstack.storage.primary.local;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageReply;
import org.zstack.header.volume.*;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;

import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public abstract class LocalStorageHypervisorBackend extends LocalStorageBase {
    public LocalStorageHypervisorBackend() {
    }

    public LocalStorageHypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion);

    abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion);

    abstract void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion);

    abstract void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion);

    abstract void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion);

    abstract void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion);

    abstract void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion);

    abstract void handle(InitPrimaryStorageOnHostConnectedMsg msg, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(TakeSnapshotMsg msg, String hostUuid, ReturnValueCompletion<TakeSnapshotReply> completion);

    abstract void handle(CheckSnapshotMsg msg, String hostUuid, Completion completion);

    abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion);

    abstract void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion);

    abstract void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, String hostUuid, Completion completion);

    abstract void handle(LocalStorageCreateEmptyVolumeMsg msg, ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply> completion);

    abstract void handle(LocalStorageDirectlyDeleteBitsMsg msg, String hostUuid, ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply> completion);

    abstract void handle(CreateTemporaryVolumeFromSnapshotMsg msg, String hostUuid, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, String huuid, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion);

    abstract void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(UploadBitsFromLocalStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply> completion);

    abstract void handle(GetVolumeRootImageUuidFromPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion);

    abstract void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> returnValueCompletion);

    abstract void handleHypervisorSpecificMessage(LocalStorageHypervisorSpecificMessage msg);

    abstract void downloadImageToCache(ImageInventory img, String hostUuid, ReturnValueCompletion<String> completion);

    abstract void handle(LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply> completion);

    abstract void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion);

    abstract void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> completion);

    abstract void handle(UnlinkBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply> completion);

    abstract List<Flow> createMigrateBitsVolumeFlow(MigrateBitsStruct struct);

    abstract void deleteBits(String path, String hostUuid, Completion completion);

    abstract void createEmptyVolume(VolumeInventory volume, String hostUuid, ReturnValueCompletion<VolumeStats> completion);

    abstract void createEmptyVolumeWithBackingFile(VolumeInventory volume, String hostUuid, String backingFile, ReturnValueCompletion<VolumeStats> completion);

    abstract void checkHostAttachedPSMountPath(String hostUuid, ReturnValueCompletion<LocalStorageKvmBackend.CheckInitializedFileRsp> completion);

    abstract void initializeHostAttachedPSMountPath(String hostUuid, Completion completion);

    abstract void handle(GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion);

    abstract void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion);

    abstract void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(UpdateVmInstanceMetadataOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply> completion);

    abstract void handle(GetVmInstanceMetadataFromPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply> completion);

    abstract void handle(ScanVmInstanceMetadataFromPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply> completion);

    abstract void handle(CleanupVmInstanceMetadataOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply> completion);

    abstract void handle(CleanupAllVmMetadataOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CleanupAllVmMetadataOnPrimaryStorageReply> completion);

    abstract void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply> completion);
}
