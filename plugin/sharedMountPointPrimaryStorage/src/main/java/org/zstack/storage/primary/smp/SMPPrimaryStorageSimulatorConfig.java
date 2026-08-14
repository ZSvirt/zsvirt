package org.zstack.storage.primary.smp;

import org.zstack.storage.primary.smp.KvmBackend.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/27.
 */
public class SMPPrimaryStorageSimulatorConfig {
    public List<ConnectCmd> connectCmds = new ArrayList<ConnectCmd>();
    public volatile long totalCapacity;
    public volatile long availableCapcacity;
    public List<CreateVolumeFromCacheCmd> createVolumeFromCacheCmds = new ArrayList<CreateVolumeFromCacheCmd>();
    public List<DeleteBitsCmd> deleteBitsCmds = new ArrayList<DeleteBitsCmd>();
    public List<CreateTemplateFromVolumeCmd> createTemplateFromVolumeCmds = new ArrayList<CreateTemplateFromVolumeCmd>();
    public List<SftpUploadBitsCmd> uploadBitsCmds = new ArrayList<SftpUploadBitsCmd>();
    public List<SftpDownloadBitsCmd> downloadBitsCmds = new ArrayList<SftpDownloadBitsCmd>();
    public List<RevertVolumeFromSnapshotCmd> revertVolumeFromSnapshotCmds = new ArrayList<RevertVolumeFromSnapshotCmd>();
    public List<MergeSnapshotCmd> mergeSnapshotCmds = new ArrayList<MergeSnapshotCmd>();
    public List<OfflineMergeSnapshotCmd> offlineMergeSnapshotCmds = new ArrayList<OfflineMergeSnapshotCmd>();
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    public List<CheckBitsCmd> checkBitsCmds = new ArrayList<CheckBitsCmd>();
    public Map<String, Long> mergeSnapshotCmdSize = new HashMap<String, Long>();
    public Map<String, Long> mergeSnapshotCmdActualSize = new HashMap<String, Long>();
    public List<GetVolumeSizeCmd> getVolumeSizeCmds = new ArrayList<GetVolumeSizeCmd>();
    public Map<String, Long> getVolumeSizeCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> getVolumeSizeCmdSize = new HashMap<String, Long>();
}
