package org.zstack.storage.primary.sharedblock;

import org.zstack.core.Platform;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.ReloadableCommand;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.VmMetadataScanEntry;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.storage.volume.VolumeErrors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmBackend.buildQcow2Options;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmBackend.getFailIfNoPath;

public class SharedBlockKvmCommands {
    public static final String PING_PATH = "/sharedblock/ping";
    @AcquireExVgLock
    public static final String CONNECT_PATH = "/sharedblock/connect";
    @AcquireExVgLock
    public static final String DISCONNECT_PATH = "/sharedblock/disconnect";
    @AcquireExVgLock
    public static final String TAKEOVER_PATH = "/sharedblock/takeover";
    @AcquireExVgLock
    public static final String CREATE_VOLUME_FROM_CACHE_PATH = "/sharedblock/createrootvolume";
    @AcquireExVgLock
    public static final String DELETE_BITS_PATH = "/sharedblock/bits/delete";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/sharedblock/createtemplatefromvolume";
    public static final String ESTIMATE_TEMPLATE_SIZE_PATH = "/sharedblock/estimatetemplatesize";
    public static final String CREATE_IMAGE_CACHE_FROM_VOLUME_PATH = "/sharedblock/createimagecachefromvolume";
    public static final String UPLOAD_BITS_TO_SFTP_BACKUPSTORAGE_PATH = "/sharedblock/sftp/upload";
    public static final String DOWNLOAD_BITS_FROM_SFTP_BACKUPSTORAGE_PATH = "/sharedblock/sftp/download";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/sharedblock/volume/revertfromsnapshot";
    public static final String MERGE_SNAPSHOT_PATH = "/sharedblock/snapshot/merge";
    public static final String EXTEND_MERGE_TARGET_PATH = "/sharedblock/snapshot/extendmergetarget";
    public static final String EXTEND_LOGICAL_VOLUME_PATH = "/sharedblock/logicalvolume/extend";
    public static final String OFFLINE_MERGE_SNAPSHOT_PATH = "/sharedblock/snapshot/offlinemerge";
    public static final String OFFLINE_COMMIT_SNAPSHOT_PATH = "/sharedblock/snapshot/offlinecommit";
    @AcquireExVgLock
    public static final String CREATE_EMPTY_VOLUME_PATH = "/sharedblock/volume/createempty";
    @AcquireExVgLock
    public static final String CREATE_DATA_VOLUME_WITH_BACKING_PATH = "/sharedblock/volume/createwithbacking";
    public static final String CHECK_BITS_PATH = "/sharedblock/bits/check";
    public static final String GET_VOLUME_SIZE_PATH = "/sharedblock/volume/getsize";
    public static final String BATCH_GET_VOLUME_SIZE_PATH = "/sharedblock/volume/batchgetsize";
    public static final String CHANGE_VOLUME_ACTIVE_PATH = "/sharedblock/volume/active";
    public static final String CONVERT_IMAGE_TO_VOLUME = "/sharedblock/image/tovolume";
    public static final String CHECK_DISKS_PATH = "/sharedblock/disks/check";
    @AcquireExVgLock
    public static final String ADD_SHARED_BLOCK = "/sharedblock/disks/add";
    @AcquireExVgLock
    public static final String RESIZE_VOLUME_PATH = "/sharedblock/volume/resize";
    public static final String MIGRATE_DATA_PATH = "/sharedblock/volume/migrate";
    public static final String GET_BLOCK_DEVICES_PATH = "/sharedblock/blockdevices";
    public static final String KVM_HA_CANCEL_SELF_FENCER = "/ha/sharedblock/cancelselffencer";
    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/sharedblock/setupselffencer";
    public static final String DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/sharedblock/kvmhost/download";
    public static final String CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/sharedblock/kvmhost/download/cancel";
    public static final String GET_BACKING_CHAIN_PATH = "/sharedblock/volume/backingchain";
    public static final String CONVERT_VOLUME_PROVISIONING_PATH = "/sharedblock/volume/convertprovisioning";
    public static final String CONFIG_FILTER_PATH = "/sharedblock/disks/filter";
    public static final String CONVERT_VOLUME_FORMAT_PATH = "/sharedblock/volume/convertformat";
    public static final String GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH = "/sharedblock/kvmhost/download/progress";
    public static final String SHRINK_SNAPSHOT_PATH = "/sharedblock/snapshot/shrink";
    public static final String GET_QCOW2_HASH_VALUE_PATH = "/sharedblock/getqcow2hash";
    public static final String CHECK_STATE_PATH = "/sharedblock/vgstate/check";
    public static final String VGS_ALL_PATH = "/sharedblock/vgs/all";
    public static final String VGS_MANAGED_PATH = "/sharedblock/vgs/managed";
    public static final String WRITE_VM_METADATA_PATH = "/sharedblock/vm/metadata/write";
    public static final String GET_VM_INSTANCE_METADATA_PATH = "/sharedblock/vm/metadata/get";
    public static final String SCAN_VM_METADATA_PATH = "/sharedblock/vm/metadata/scan";
    public static final String CLEANUP_VM_METADATA_PATH = "/sharedblock/vm/metadata/cleanup";
    public static final String PREFIX_REBASE_BACKING_FILES_PATH = "/sharedblock/snapshot/prefixrebasebackingfiles";

    public static class AgentCmd extends KVMAgentCommands.PrimaryStorageCommand {
        public String vgUuid;
        public String hostUuid;
        public String provisioning;
        public Map<String, Object> addons;

        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }
    }

    public static class Qcow2Cmd extends AgentCmd {
        public String qcow2Options = buildQcow2Options();
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
        public List<LunCapacity> lunCapacities;

        public void setError(String error) {
            success = false;
            this.error = error;
        }

        protected ErrorCode buildErrorCode() {
            if (success) {
                return null;
            }
            return operr("operation error, because:%s", error);
        }
    }

    public static class CheckLockCmd extends AgentCmd {
        public List<String> vgUuids;
    }

    public static class CheckDisksCmd extends AgentCmd {
        public List<String> sharedBlockUuids;
        public boolean rescan = false;
        public boolean rescan_scsi = false;
        public boolean failIfNoPath = getFailIfNoPath();
    }

    public static class ConfigFilterCmd extends AgentCmd {
        public List<String> allSharedBlockUuids;
    }

    public static class ConnectCmd extends AgentCmd {
        public List<String> sharedBlockUuids;
        public String hostId;
        public boolean forceWipe = false;
        public boolean enableLvmetad = false;
        public List<String> allSharedBlockUuids;
        public Long ioTimeout = 40l;
        public Long maxActualSizeFactor = 3l;
        public boolean isFirst;
    }

    public static class ConnectRsp extends AgentRsp {
        public boolean isFirst = false;
        public String hostId;
        public String vgLvmUuid;
        public String hostUuid;
    }

    public static class TakeoverCmd extends AgentCmd {
        public List<String> sharedBlockUuids;
        public List<String> allSharedBlockUuids;
        public String hostId;
        public Long ioTimeout;
        public boolean enableLvmetad = false;
    }

    public static class TakeoverRsp extends AgentRsp {
    }

    public static class CheckLockRsp extends AgentRsp {
        public Map<String, String> failedVgs;
    }

    public static class ActivateRsp extends AgentRsp {
        public boolean inUse;
        public ErrorCode buildErrorCode() {
            if (inUse) {
                return Platform.err(VolumeErrors.VOLUME_IN_USE, error);
            }
            return super.buildErrorCode();
        }
    }

    public static class DisonnectCmd extends AgentCmd {
        public boolean stopServices;
    }

    public static class AddDiskCmd extends AgentCmd {
        public String diskUuid;
        public List<String> allSharedBlockUuids;
        public boolean forceWipe = false;
        public boolean onlyGenerateFilter = false;
    }

    public static class CreateVolumeFromCacheCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd, HasThreadContext {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
        public long virtualSize;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }
    }

    public static class CreateVolumeFromCacheRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }
    public static class DeleteBitsCmd extends AgentCmd {
        public String path;
        public boolean folder = false;
        public String issueDiscards;
    }

    public static class DeleteTag extends AgentCmd {
        public String tag;
    }

    public static class CreateTemplateFromVolumeCmd extends AgentCmd implements HasThreadContext {
        public String installPath;
        public String volumePath;
        public boolean sharedVolume = false;
        public boolean compareQcow2 = false;
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

    public static class CreateImageCacheFromVolumeCmd extends AgentCmd implements HasThreadContext {
        public String installPath;
        public String volumePath;
        public boolean compareQcow2 = false;
    }

    public static class CreateImageCacheFromVolumeRsp extends AgentRsp {
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
        public int lockType = LvmlockdLockingType.SHARE.getValue();
        public String hostname;
        public String username;
        public String backupStorageInstallPath;
        public String primaryStorageInstallPath;
    }

    public static class RevertVolumeFromSnapshotCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd {
        public String snapshotInstallPath;
        public String volumeUuid;
        public String installPath;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }
    }

    public static class RevertVolumeFromSnapshotRsp extends AgentRsp {
        @Validation
        public String newVolumeInstallPath;

        @Validation
        public long size;
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

    public static class ExtendMergeTargetCmd extends AgentCmd {
        public String srcPath;
        public String destPath;
        public boolean fullRebase;
        public String volumeUuid;
    }

    public static class ExtendLogicalVolumeCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd {
        public String destPath;
        public String volumeUuid;
        public long requiredSize;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return destPath;
        }
    }

    public static class ExtendMergeTargetRsp extends AgentRsp {
        public long size;
    }

    public static class ExtendLogicalVolumeRsp extends AgentRsp {
    }

    public static class OfflineMergeSnapshotCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd, HasThreadContext {
        public String srcPath;
        public String destPath;
        public boolean fullRebase;
        public boolean sharedVolume = false;
        public String volumeUuid;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return srcPath;
        }
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

    public static class OfflineCommitSnapshotCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd, HasThreadContext {
        public String top;
        public String base;
        public String volumeUuid;
        public List<String> topChildrenInstallPathInDb = new ArrayList<>();

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return top;
        }
    }

    public static class OfflineCommitSnapshotRsp extends AgentRsp {
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class CreateEmptyVolumeCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd, HasThreadContext {
        public String installPath;
        public long size;
        public String name;
        public String volumeUuid;
        public String volumeFormat;
        public String backingFile;
        public boolean zeroFilled = true;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static class CreateDataVolumeWithBackingCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd, HasThreadContext {
        public String templatePathInCache;
        public String installPath;
        public String volumeUuid;
        public long virtualSize;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }
    }

    public static class CreateDataVolumeWithBackingRsp extends AgentRsp {
        public long actualSize;
        public long size;
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

    public static class GetBatchVolumeSizeCmd extends AgentCmd {
        public Map<String, String> volumeUuidInstallPaths;
    }

    public static class GetVolumeSizeRsp extends AgentRsp {
        public Long actualSize;
        public Long size;
    }

    public static class GetBatchVolumeSizeRsp extends AgentRsp {
        public Map<String, Long> actualSizes = new HashMap<>();
    }

    public static class ActiveVolumeCmd extends AgentCmd {
        public String installPath;
        public int lockType;
        public boolean recursive = true;
        public boolean killProcess = false;
    }

    public static class ConvertImageToVolumeCmd extends AgentCmd {
        public String primaryStorageInstallPath;
    }

    public static class ResizeVolumeCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd {
        public String installPath;
        public long size;
        public boolean force = false;
        public boolean live = false;
        public String volumeUuid;

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }
    }

    public static class ResizeVolumeRsp extends AgentRsp {
        public long size;
    }

    public static class ConvertVolumeFormatCmd extends AgentCmd {
        public String srcFormat;
        public String dstFormat;
        public String installPath;
    }

    public static class MigrateDataCmd extends AgentCmd implements HasThreadContext {
        public List<SharedBlockMigrateVolumeStruct> migrateVolumeStructs;
        public String volumePath;
    }

    public static class GetBlockDevicesCmd extends AgentCmd {
    }

    public static class GetBlockDevicesRsp extends AgentRsp {
        List<BlockDeviceStruct> blockDevices;

        public List<BlockDeviceStruct> getBlockDevices() {
            return blockDevices;
        }

        public void setBlockDevices(List<BlockDeviceStruct> blockDevices) {
            this.blockDevices = blockDevices;
        }
    }

    public static class KvmSetupSelfFencerCmd extends AgentCmd  {
        public long interval;
        public int maxAttempts;
        public int storageCheckerTimeout;
        public boolean fail_if_no_path = SharedBlockGlobalConfig.FAIL_IF_MULTIPATH_NO_PATH.value().equals("true");
        public boolean checkIo = SharedBlockGlobalConfig.CHECK_IO_FENCER.value().equals("true");
        public String strategy;
        public List<String> fencers;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCmd {
    }

    public static class DownloadBitsFromKVMHostRsp extends AgentRsp {
        public String format;
    }

    public static class DownloadBitsFromKVMHostCmd extends AgentCmd implements ReloadableCommand {
        private String hostname;
        private String username;
        private String sshKey;
        private int sshPort;
        private int lockType = LvmlockdLockingType.SHARE.getValue();
        // it's file path on kvm host actually
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;
        private Long bandWidth;
        private String identificationCode;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public int getLockType() {
            return lockType;
        }

        public void setLockType(int lockType) {
            this.lockType = lockType;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public Long getBandWidth() {
            return bandWidth;
        }

        public void setBandWidth(Long bandWidth) {
            this.bandWidth = bandWidth;
        }

        @Override
        public void setIdentificationCode(String identificationCode) {
            this.identificationCode = identificationCode;
        }
    }

    public static class CancelDownloadBitsFromKVMHostCmd extends AgentCmd {
        private int lockType = LvmlockdLockingType.SHARE.getValue();
        private String primaryStorageInstallPath;


        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class GetDownloadBitsFromKVMHostProgressCmd extends AgentCmd {
        public List<String> volumePaths;
    }

    public static class GetDownloadBitsFromKVMHostProgressRsp extends AgentRsp {
        public long totalSize;
    }

    public static class GetBackingChainCmd extends AgentCmd {
        public String volumeUuid;
        public String installPath;
        public boolean containSelf = true;
    }

    public static class GetBackingChainRsp extends AgentRsp {
        public List<String> backingChain;
        public long totalSize;
    }

    public static class ConvertVolumeProvisioningCmd extends AgentCmd implements ProvisionSharedBlockVolumeCmd {
        public String provisioningStrategy;
        public String installPath;
        public String volumeUuid;

        public String getProvisioningStrategy() {
            return provisioningStrategy;
        }

        public void setProvisioningStrategy(String provisioningStrategy) {
            this.provisioningStrategy = provisioningStrategy;
        }

        @Override
        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        @Override
        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public static class ConvertVolumeProvisioningRsp extends AgentRsp {
        public Long actualSize;
    }

    public static class ShrinkSnapshotCmd extends AgentCmd {
        public String installPath;
    }

    public static class ShrinkSnapshotRsp extends AgentRsp {
        public Long oldSize;
        public Long size;
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

    public static class GetVgsInfoCmd extends AgentCmd {
    }

    public static class GetVgsInfoRsp extends AgentRsp {
        public Map<String, SharedBlockGroupDiskInfo> groupDiskInfos = new HashMap<>();
    }

    public static class GetManagedVgsInfoCmd extends AgentCmd {
    }

    public static class GetManagedVgsInfoRsp extends AgentRsp {
        public Map<String, SharedBlockGroupDiskInfo> groupDiskInfos = new HashMap<>();
    }

    public static class SharedBlockGroupDiskInfo {
        public List<SharedBlockCandidateStruct> disks;
        public Long diskCount;
    }

    public static class GetVmInstanceMetadataCmd extends AgentCmd {
        private String metadataPath;

        public String getMetadataPath() {
            return metadataPath;
        }

        public void setMetadataPath(String metadataPath) {
            this.metadataPath = metadataPath;
        }
    }

    public static class GetVmInstanceMetadataRsp extends AgentRsp {
        private String metadata;

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }

    public static class ScanVmMetadataCmd extends AgentCmd {
        public String metadataDir;
    }

    public static class ScanVmMetadataRsp extends AgentRsp {
        private List<VmMetadataScanEntry> metadataEntries = new ArrayList<>();

        public List<VmMetadataScanEntry> getMetadataEntries() {
            return metadataEntries;
        }

        public void setMetadataEntries(List<VmMetadataScanEntry> metadataEntries) {
            this.metadataEntries = metadataEntries;
        }
    }

    public static class CleanupVmMetadataCmd extends AgentCmd {
        private String metadataPath;

        public String getMetadataPath() {
            return metadataPath;
        }

        public void setMetadataPath(String metadataPath) {
            this.metadataPath = metadataPath;
        }
    }

    public static class CleanupVmMetadataRsp extends AgentRsp {
    }

    public static class WriteVmMetadataCmd extends AgentCmd {
        public String metadata;
        public String metadataPath;
        public String vmUuid;
        public String vmName;
        public String vmCategory;
        public String architecture;
        public String schemaVersion;
    }

    public static class WriteVmMetadataRsp extends AgentRsp {
    }

    public static class PrefixRebaseBackingFilesCmd extends AgentCmd {
        public List<String> filePaths;
        public String oldPrefix;
        public String newPrefix;
    }

    public static class PrefixRebaseBackingFilesRsp extends AgentRsp {
        public int rebasedCount;
    }
}
