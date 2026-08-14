package org.zstack.simulator.storage.primary.nfs;

import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.*;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NfsPrimaryStorageSimulatorConfig {
    public volatile boolean createRootVolumeFromTemplateSuccess = true;
    public volatile boolean createRootVolumeFromTemplateException;
    public volatile long totalCapacity = SizeUnit.GIGABYTE.toByte(100);
    public volatile long availableCapacity = SizeUnit.GIGABYTE.toByte(100);
    public volatile boolean mountException = false;
    public volatile boolean mountSuccess = true;
    public volatile List<MountCmd> mountCmds = new ArrayList<MountCmd>();
    public volatile List<UnmountCmd> unmountCmds = new ArrayList<UnmountCmd>();
    public volatile boolean unmountSuccess = true;
    public volatile boolean unmountException = false;
    public volatile boolean createEmptyVolumeSuccess = true;
    public volatile boolean createTemplateFromRootVolumeSuccess = true;
    public volatile List<String> imageCache = new ArrayList<String>();
    public volatile boolean checkImageSuccess = true;
    public volatile boolean uploadToSftp = true;
    public volatile String backupSnapshotFailurePrimaryStorageInstallPath = null;
    public volatile List<UploadToSftpCmd> uploadToSftpCmds = new ArrayList<UploadToSftpCmd>();
    public volatile boolean downloadFromSftpSuccess = true;
    public volatile List<DownloadBitsFromSftpBackupStorageCmd> downloadFromSftpCmds = new ArrayList<DownloadBitsFromSftpBackupStorageCmd>();
    public volatile boolean deleteSuccess = true;
    public volatile List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public volatile boolean mergeSnapshotSuccess = true;
    public volatile List<MergeSnapshotCmd> mergeSnapshotCmds = new ArrayList<MergeSnapshotCmd>();
    public volatile boolean rebaseAndMergeSnapshotSuccess = true;
    public volatile List<RebaseAndMergeSnapshotsCmd> rebaseAndMergeSnapshotsCmds = new ArrayList<RebaseAndMergeSnapshotsCmd>();
    public volatile boolean revertVolumeFromSnapshotSuccess = true;
    public volatile List<RevertVolumeFromSnapshotCmd> revertVolumeFromSnapshotCmds = new ArrayList<RevertVolumeFromSnapshotCmd>();
    public volatile List<MoveBitsCmd> moveBitsCmds = new ArrayList<MoveBitsCmd>();
    public volatile boolean moveBitsSuccess = true;
    public volatile boolean offlineMergeSnapshotSuccess = true;
    public volatile List<OfflineMergeSnapshotCmd> offlineMergeSnapshotCmds = new ArrayList<OfflineMergeSnapshotCmd>();
    public volatile List<RemountCmd> remountCmds = new ArrayList<RemountCmd>();
    public volatile boolean remountSuccess = true;

    public Map<String, Long> mergeSnapshotCmdSize = new HashMap<String, Long>();
    public Map<String, Long> mergeSnapshotCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> rebaseAndMergeSnapshotsCmdSize = new HashMap<String, Long>();
    public Map<String, Long> rebaseAndMergeSnapshotsCmdActualSize = new HashMap<String, Long>();
    public List<GetVolumeActualSizeCmd> getVolumeSizeCmds = new ArrayList<GetVolumeActualSizeCmd>();
    public Map<String, Long> getVolumeSizeCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> getVolumeSizeCmdSize = new HashMap<String, Long>();
    public volatile boolean pingSuccess = true;
    public List<PingCmd> pingCmds = new ArrayList<PingCmd>();

    public Map<String, String> getVolumeBaseImagePaths = new HashMap<String, String>();
    public List<UpdateMountPointCmd> updateMountPointCmds = new ArrayList<>();
}
