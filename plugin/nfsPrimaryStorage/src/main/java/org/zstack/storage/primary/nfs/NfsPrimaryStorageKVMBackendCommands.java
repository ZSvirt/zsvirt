package org.zstack.storage.primary.nfs;

import org.zstack.header.HasThreadContext;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.storage.primary.VmMetadataScanEntry;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NfsPrimaryStorageKVMBackendCommands {
    public static class NfsPrimaryStorageAgentResponse extends AgentResponse {
        private Long totalCapacity;
        private Long availableCapacity;

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }
    }

    public static class NfsPrimaryStorageAgentCommand extends KVMAgentCommands.PrimaryStorageCommand {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
            this.primaryStorageUuid = uuid;
        }
    }

    public static class DownloadBitsFromKVMHostRsp extends AgentResponse {
        public String format;
    }

    public static class DownloadBitsFromKVMHostCmd extends AgentCommand {
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

    public static class CancelDownloadBitsFromKVMHostCmd extends AgentCommand {
        public String primaryStorageInstallPath;
    }

    public static class GetDownloadBitsFromKVMHostProgressCmd extends AgentCommand {
        public List<String> volumePaths;
    }

    public static class GetDownloadBitsFromKVMHostProgressRsp extends AgentResponse {
        public long totalSize;
    }

    public static class MountCmd extends NfsPrimaryStorageAgentCommand {
        private String url;
        private String mountPath;
        private String options;

        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }

        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getMountPath() {
            return mountPath;
        }
        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }

    
    public static class MountAgentResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class GetCapacityCmd extends NfsPrimaryStorageAgentCommand {
        private String mountPath;

        public String getMountPath() {
            return mountPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }
    
    public static class GetCapacityResponse extends NfsPrimaryStorageAgentResponse {
    }
    
    public static class UnmountCmd extends NfsPrimaryStorageAgentCommand {
        private String mountPath;
        private String url;
        
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getMountPath() {
            return mountPath;
        }
        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }
    public static class UnmountResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class CreateTemplateFromVolumeCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext{
        private String installPath;
        private String rootVolumePath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
        public String getRootVolumePath() {
            return rootVolumePath;
        }
        public void setVolumePath(String rootVolumePath) {
            this.rootVolumePath = rootVolumePath;
        }
    }
    public static class CreateTemplateFromVolumeRsp extends NfsPrimaryStorageAgentResponse {
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class EstimateTemplateSizeCmd extends NfsPrimaryStorageAgentCommand {
        private String volumePath;

        public String getVolumePath() {
            return volumePath;
        }

        public void setVolumePath(String volumePath) {
            this.volumePath = volumePath;
        }
    }

    public static class EstimateTemplateSizeRsp extends NfsPrimaryStorageAgentResponse {
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class DownloadBitsFromSftpBackupStorageCmd extends NfsPrimaryStorageAgentCommand {
        private String sshKey;
        private String hostname;
        private String username;
        private int sshPort;
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public int getSshPort() {
            return sshPort;
        }
        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }
        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
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
    }

    public static class DownloadBitsFromSftpBackupStorageResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class CheckIsBitsExistingCmd extends NfsPrimaryStorageAgentCommand {
        private String installPath;
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class CheckIsBitsExistingRsp extends NfsPrimaryStorageAgentResponse {
        private boolean existing;

        public boolean isExisting() {
            return existing;
        }

        public void setExisting(boolean existing) {
            this.existing = existing;
        }
    }

    public static class CreateFolderCmd extends NfsPrimaryStorageAgentCommand {
        private String installUrl;

        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
    }

    public abstract static class CreateVolumeCmd extends NfsPrimaryStorageAgentCommand {
        private String installUrl;
        private String accountUuid;
        private String hypervisorType;
        private String name;
        private String volumeUuid;
        private String volumeFormat = VolumeConstant.VOLUME_FORMAT_QCOW2;
        protected long  virtualSize;

        public String getInstallUrl() {
            return installUrl;
        }
        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
        public String getAccountUuid() {
            return accountUuid;
        }
        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
        public String getHypervisorType() {
            return hypervisorType;
        }
        public void setHypervisorType(String hypervisorType) {
            this.hypervisorType = hypervisorType;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getVolumeUuid() {
            return volumeUuid;
        }
        public void setVolumeUuid(String uuid) {
            this.volumeUuid = uuid;
        }
        public String getVolumeFormat() {
            return volumeFormat;
        }
        public void setVolumeFormat(String volumeFormat) {
            this.volumeFormat = volumeFormat;
        }
        public long getVirtualSize() {
            return virtualSize;
        }
        public void setVirtualSize(long virtualSize) {
            this.virtualSize = virtualSize;
        }
    }

    public static class CreateRootVolumeFromTemplateCmd extends CreateVolumeCmd {
        private String templatePathInCache;
        private long timeout;

        public long getTimeout() {
            return timeout;
        }
        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
        public String getTemplatePathInCache() {
            return templatePathInCache;
        }
        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }
    }
    
    public static class CreateRootVolumeFromTemplateResponse extends NfsPrimaryStorageAgentResponse {
        public Long actualSize;
        public Long size;
    }

    public static class CreateVolumeWithBackingCmd extends CreateVolumeCmd {
        private String templatePathInCache;

        public String getTemplatePathInCache() {
            return templatePathInCache;
        }
        public void setTemplatePathInCache(String templatePathInCache) {
            this.templatePathInCache = templatePathInCache;
        }
    }

    public static class CreateVolumeWithBackingRsp extends NfsPrimaryStorageAgentResponse {
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

    }

    public static class CreateEmptyVolumeCmd extends CreateVolumeCmd {
        private long size;
        private boolean withoutVolume;
        private String backingFile;

        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }

        public boolean isWithoutVolume() {
            return withoutVolume;
        }

        public void setWithoutVolume(boolean withoutVolume) {
            this.withoutVolume = withoutVolume;
        }

        public String getBackingFile() {
            return backingFile;
        }

        public void setBackingFile(String backingFile) {
            this.backingFile = backingFile;
        }
    }
    public static class CreateEmptyVolumeResponse extends NfsPrimaryStorageAgentResponse {
        public Long actualSize;
        public Long size;
    }

    public static class DeleteCmd extends NfsPrimaryStorageAgentCommand {
        private boolean folder;
        private String installPath;

        public boolean isFolder() {
            return folder;
        }

        public void setFolder(boolean isFolder) {
            this.folder = isFolder;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteResponse extends NfsPrimaryStorageAgentResponse {
        public boolean inUse;
    }

    public static class UnlinkBitsCmd extends NfsPrimaryStorageAgentCommand {
        public String installPath;
        public boolean onlyLinkedFile = true;
    }

    public static class UnlinkBitsRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class ListDirectionCmd extends NfsPrimaryStorageAgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class ListDirectionResponse extends NfsPrimaryStorageAgentResponse {
        private List<String> paths;

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
    
    public static class RevertVolumeFromSnapshotCmd extends NfsPrimaryStorageAgentCommand {
        private String snapshotInstallPath;

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }
    }

    public static class RevertVolumeFromSnapshotResponse extends NfsPrimaryStorageAgentResponse {
        @Validation
        private String newVolumeInstallPath;

        @Validation
        private long size;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class ReInitImageCmd extends NfsPrimaryStorageAgentCommand {
        private String imagePath;
        private String volumePath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getVolumePath() {
            return volumePath;
        }

        public void setVolumePath(String volumePath) {
            this.volumePath = volumePath;
        }
    }

    public static class ReInitImageRsp extends NfsPrimaryStorageAgentResponse {
        @Validation
        private String newVolumeInstallPath;

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class UploadToSftpCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext{
        private String primaryStorageInstallPath;
        private String backupStorageInstallPath;
        private String backupStorageHostName;
        private String backupStorageUserName;
        private String backupStorageSshKey;
        private int backupStorageSshPort;
        public String getBackupStorageUserName() {
            return backupStorageUserName;
        }
        public void setBackupStorageUserName(String backupStorageUserName) {
            this.backupStorageUserName = backupStorageUserName;
        }
        public void setBackupStorageSshPort(int backupStorageSshPort) {
            this.backupStorageSshPort = backupStorageSshPort;
        }
        public int getBackupStorageSshPort() {
            return backupStorageSshPort;
        }
        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getBackupStorageHostName() {
            return backupStorageHostName;
        }

        public void setBackupStorageHostName(String backupStorageHostName) {
            this.backupStorageHostName = backupStorageHostName;
        }

        public String getBackupStorageSshKey() {
            return backupStorageSshKey;
        }

        public void setBackupStorageSshKey(String backupStorageSshKey) {
            this.backupStorageSshKey = backupStorageSshKey;
        }
    }

    public static class UploadToSftpResponse extends NfsPrimaryStorageAgentResponse {
    }

    public static class MergeSnapshotCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        private String volumeUuid;
        private String snapshotInstallPath;
        private String workspaceInstallPath;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }

        public String getWorkspaceInstallPath() {
            return workspaceInstallPath;
        }

        public void setWorkspaceInstallPath(String workspaceInstallPath) {
            this.workspaceInstallPath = workspaceInstallPath;
        }
    }

    public static class MergeSnapshotResponse extends NfsPrimaryStorageAgentResponse {
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class RebaseAndMergeSnapshotsCmd extends NfsPrimaryStorageAgentCommand {
        private String volumeUuid;
        private List<String> snapshotInstallPaths;
        private String workspaceInstallPath;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public List<String> getSnapshotInstallPaths() {
            return snapshotInstallPaths;
        }

        public void setSnapshotInstallPaths(List<String> snapshotInstallPaths) {
            this.snapshotInstallPaths = snapshotInstallPaths;
        }

        public String getWorkspaceInstallPath() {
            return workspaceInstallPath;
        }

        public void setWorkspaceInstallPath(String workspaceInstallPath) {
            this.workspaceInstallPath = workspaceInstallPath;
        }
    }

    public static class RebaseAndMergeSnapshotsResponse extends NfsPrimaryStorageAgentResponse {
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class MoveBitsCmd extends NfsPrimaryStorageAgentCommand {
        private String srcPath;
        private String destPath;

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDestPath() {
            return destPath;
        }

        public void setDestPath(String destPath) {
            this.destPath = destPath;
        }
    }

    public static class MoveBitsRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class OfflineMergeSnapshotCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        private String srcPath;
        private String destPath;
        private boolean fullRebase;

        public boolean isFullRebase() {
            return fullRebase;
        }

        public void setFullRebase(boolean fullRebase) {
            this.fullRebase = fullRebase;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDestPath() {
            return destPath;
        }

        public void setDestPath(String destPath) {
            this.destPath = destPath;
        }
    }

    public static class OfflineMergeSnapshotRsp extends NfsPrimaryStorageAgentResponse {
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class OfflineCommitSnapshotCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        public String top;
        public String base;
        public List<String> topChildrenInstallPathInDb = new ArrayList<>();
    }

    public static class OfflineCommitSnapshotRsp extends NfsPrimaryStorageAgentResponse {
        private Long actualSize;

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class RemountCmd extends NfsPrimaryStorageAgentCommand {
        public String url;
        public String mountPath;
        public String options;
    }

    public static class GetVolumeActualSizeCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetBatchVolumeActualSizeCmd extends NfsPrimaryStorageAgentCommand {
        public Map<String, String> volumeUuidInstallPaths;
    }

    public static class GetVolumeActualSizeRsp extends NfsPrimaryStorageAgentResponse {
        public long actualSize;
        public long size;
    }

    public static class GetBatchVolumeActualSizeRsp extends NfsPrimaryStorageAgentResponse {
        public Map<String, Long> actualSizes;
    }

    public static class PingCmd extends NfsPrimaryStorageAgentCommand {
        public String mountPath;
        public String url;
    }

    /**
     * volumeInstallDir contains all snapshots and base volume file,
     * their backing file in imageCacheDir will be identified as the base image.
     * This takes into consideration multiple snapshot chains and chain-based image.
     */
    public static class GetVolumeBaseImagePathCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUuid;
        public String volumeInstallDir;
        public String imageCacheDir;
        public String volumeInstallPath;
    }

    public static class GetVolumeBaseImagePathRsp extends NfsPrimaryStorageAgentResponse {
        public String path;
        public List<String> otherPaths;
    }

    public static class GetBackingChainCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetBackingChainRsp extends NfsPrimaryStorageAgentResponse {
        public List<String> backingChain;
        public long totalSize;
    }

    public static class UpdateMountPointCmd extends NfsPrimaryStorageAgentCommand {
        public String oldMountPoint;
        public String newMountPoint;
        public String mountPath;
        public String options;
    }

    public static class UpdateMountPointRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class NfsToNfsMigrateBitsCmd extends NfsPrimaryStorageAgentCommand implements HasThreadContext {
        public String srcFolderPath;
        public String dstFolderPath;
        public List<String> filtPaths;

        public String independentPath;
        public boolean isMounted = false;
        public String url;
        public String options;
        public String mountPath;
        public String volumeInstallPath;
        public String srcPrimaryStorageUuid;
    }

    public static class NfsToNfsMigrateBitsRsp extends NfsPrimaryStorageAgentResponse {
        public Map<String, Long> dstFilesActualSize;
    }

    public static class NfsRebaseVolumeBackingFileCmd extends NfsPrimaryStorageAgentCommand {
        public String srcPsMountPath;
        public String dstPsMountPath;
        public String dstVolumeFolderPath;
        public String dstImageCacheTemplateFolderPath;
    }

    public static class NfsRebaseVolumeBackingFileRsp extends NfsPrimaryStorageAgentResponse {

    }

    public static class LinkVolumeNewDirCmd extends NfsPrimaryStorageAgentCommand {
        public String volumeUuid;
        public String srcDir;
        public String dstDir;
    }

    public static class LinkVolumeNewDirRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class GetQcow2HashValueCmd extends NfsPrimaryStorageAgentCommand {
        private String hostUuid;
        private String installPath;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class GetQcow2HashValueRsp extends NfsPrimaryStorageAgentResponse {
        private String hashValue;

        public String getHashValue() {
            return hashValue;
        }

        public void setHashValue(String hashValue) {
            this.hashValue = hashValue;
        }
    }

    public static class WriteVmMetadataCmd extends NfsPrimaryStorageAgentCommand {
        public String metadata;
        public String metadataPath;
        public String vmUuid;
        public String vmName;
        public String vmCategory;
        public String architecture;
        public String schemaVersion;
    }

    public static class WriteVmMetadataRsp extends NfsPrimaryStorageAgentResponse {
    }


    public static class GetVmInstanceMetadataCmd extends NfsPrimaryStorageAgentCommand {
        public String metadataPath;
    }

    public static class GetVmInstanceMetadataRsp extends NfsPrimaryStorageAgentResponse {
        public String metadata;
    }

    public static class ScanVmMetadataCmd extends NfsPrimaryStorageAgentCommand {
        public String metadataDir;
    }

    public static class ScanVmMetadataRsp extends NfsPrimaryStorageAgentResponse {
        public List<VmMetadataScanEntry> metadataEntries = new ArrayList<>();
    }

    public static class CleanupVmMetadataCmd extends NfsPrimaryStorageAgentCommand {
        public String metadataPath;
    }

    public static class CleanupVmMetadataRsp extends NfsPrimaryStorageAgentResponse {
    }

    public static class PrefixRebaseBackingFilesCmd extends NfsPrimaryStorageAgentCommand {
        public List<String> filePaths;
        public String oldPrefix;
        public String newPrefix;
    }

    public static class PrefixRebaseBackingFilesRsp extends NfsPrimaryStorageAgentResponse {
        public int rebasedCount;
    }
}
