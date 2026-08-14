package org.zstack.storage.primary.local;

import org.zstack.storage.primary.local.LocalStorageKvmBackend.*;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageSimulatorConfig {
    public static class Capacity {
        public long total;
        public long avail;
    }

    public Map<String, Capacity> capacityMap = new HashMap<>();
    public List<InitCmd> initCmdList = new ArrayList<>();
    public List<GetPhysicalCapacityCmd> getPhysicalCapacityCmds = new ArrayList<>();
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<>();
    public List<CreateVolumeFromCacheCmd> createVolumeFromCacheCmds = new ArrayList<>();
    public List<DeleteBitsCmd> deleteBitsCmds = new ArrayList<>();
    public List<DeleteBitsCmd> deleteDirCmds = new ArrayList<>();
    public List<SftpUploadBitsCmd> uploadBitsCmds = new ArrayList<>();
    public List<SftpDownloadBitsCmd> downloadBitsCmds = new ArrayList<>();
    public List<CreateTemplateFromVolumeCmd> createTemplateFromVolumeCmds = new ArrayList<>();
    public List<RevertVolumeFromSnapshotCmd> revertVolumeFromSnapshotCmds = new ArrayList<>();
    public List<MergeSnapshotCmd> mergeSnapshotCmds = new ArrayList<>();
    public List<RebaseAndMergeSnapshotsCmd> rebaseAndMergeSnapshotsCmds = new ArrayList<>();
    public List<OfflineMergeSnapshotCmd> offlineMergeSnapshotCmds = new ArrayList<>();
    public List<CheckBitsCmd> checkBitsCmds = new ArrayList<>();
    public List<RebaseSnapshotBackingFilesCmd> rebaseSnapshotBackingFilesCmds = new ArrayList<>();
    public List<VerifySnapshotChainCmd> verifySnapshotChainCmds = new ArrayList<>();
    public List<CopyBitsFromRemoteCmd> copyBitsFromRemoteCmds = new ArrayList<>();
    public List<GetMd5Cmd> getMd5Cmds = new ArrayList<>();
    public List<CheckMd5sumCmd> checkMd5sumCmds = new ArrayList<>();
    public List<GetBackingFileCmd> getBackingFileCmds = new ArrayList<>();
    public volatile String backingFilePath;
    public volatile Long backingFileSize;
    public volatile Long baseImageSize;
    public volatile boolean checkMd5Success = true;
    public volatile boolean checkBitsSuccess = true;
    public volatile boolean copyBitsFromRemoteSuccess = true;

    public Map<String, Long> snapshotToVolumeSize = new HashMap<>();
    public Map<String, Long> snapshotToVolumeActualSize = new HashMap<>();
    public List<GetVolumeSizeCmd> getVolumeSizeCmds = new ArrayList<>();
    public Map<String, Long> getVolumeSizeCmdActualSize = new HashMap<>();
    public Map<String, Long> getVolumeSizeCmdSize = new HashMap<>();

    public Map<String, String> getVolumeBaseImagePaths = new HashMap<>();

    public List<GetQCOW2ReferenceCmd> getQCOW2ReferenceCmds = new ArrayList<>();
    public List<String> getQCOW2ReferenceCmdReference = new ArrayList<>();

    public List<WriteVmMetadataCmd> writeVmMetadataCmds = new ArrayList<>();
    public List<GetVmInstanceMetadataCmd> getVmInstanceMetadataCmds = new ArrayList<>();
    public List<ScanVmMetadataCmd> scanVmMetadataCmds = new ArrayList<>();
    public List<CleanupVmMetadataCmd> cleanupVmMetadataCmds = new ArrayList<>();
}
