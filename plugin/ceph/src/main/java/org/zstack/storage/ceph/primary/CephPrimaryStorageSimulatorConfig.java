package org.zstack.storage.ceph.primary;

import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.*;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingOperationFailure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageSimulatorConfig {
    public static class CephPrimaryStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String fsid;
    }

    public volatile boolean monInitSuccess = true;
    public Map<String, CephPrimaryStorageConfig> config = new HashMap<String, CephPrimaryStorageConfig>();
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    public List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public List<CreateSnapshotCmd> createSnapshotCmds = new ArrayList<CreateSnapshotCmd>();
    public Map<String, Long> createSnapshotCmdSize = new HashMap<String, Long>();
    public List<DeleteSnapshotCmd> deleteSnapshotCmds = new ArrayList<DeleteSnapshotCmd>();
    public List<ProtectSnapshotCmd> protectSnapshotCmds = new ArrayList<ProtectSnapshotCmd>();
    public List<UnprotectedSnapshotCmd> unprotectedSnapshotCmds = new ArrayList<UnprotectedSnapshotCmd>();
    public List<CloneCmd> cloneCmds = new ArrayList<CloneCmd>();
    public List<FlattenCmd> flattenCmds = new ArrayList<FlattenCmd>();
    public List<CpCmd> cpCmds = new ArrayList<CpCmd>();
    public List<SftpDownloadCmd> sftpDownloadCmds = new ArrayList<SftpDownloadCmd>();
    public List<SftpUpLoadCmd> sftpUpLoadCmds = new ArrayList<SftpUpLoadCmd>();
    public List<RollbackSnapshotCmd> rollbackSnapshotCmds = new ArrayList<RollbackSnapshotCmd>();
    public List<CreateKvmSecretCmd> createKvmSecretCmds = new ArrayList<CreateKvmSecretCmd>();
    public List<DeletePoolCmd> deletePoolCmds = new ArrayList<DeletePoolCmd>();
    public List<CheckIsBitsExistingCmd> checkBitsPathCmds = new ArrayList<CheckIsBitsExistingCmd>();
    public Map<String, Long> cpCmdSize = new HashMap<String, Long>();
    public Map<String, Long> cpCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> getVolumeActualSizeCmdSize = new HashMap<String, Long>();

    public List<GetVolumeSizeCmd> getVolumeSizeCmds = new ArrayList<GetVolumeSizeCmd>();
    public Map<String, Long> getVolumeSizeCmdSize = new HashMap<String, Long>();
    public Map<String, Long> getVolumeSizeCmdActualSize = new HashMap<String, Long>();

    public Map<String, Boolean> pingCmdSuccess = new HashMap<String, Boolean>();
    public Map<String, PingOperationFailure> pingCmdOperationFailure = new HashMap<String, PingOperationFailure>();
    public List<GetFactsCmd> getFactsCmds = new ArrayList<GetFactsCmd>();
    public Map<String, String> getFactsCmdFsid = new HashMap<String, String>();
    public List<KvmSetupSelfFencerCmd> kvmSetupSelfFencerCmds = new ArrayList<>();
    public List<KvmCancelSelfFencerCmd> kvmCancelSelfFencerCmds = new ArrayList<>();
    public List<DeleteImageCacheCmd> deleteImageCacheCmds = new ArrayList<>();
    public Map<String, String> monAddr = new HashMap<>();
    public boolean synchronizedCreateEmptyVolume = false;

    public List<GetDownloadBitsFromKVMHostProgressCmd> getDownLoadProgressCmds = new ArrayList<>();
}
