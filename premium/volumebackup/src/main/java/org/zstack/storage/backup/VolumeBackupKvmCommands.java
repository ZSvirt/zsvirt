package org.zstack.storage.backup;

import org.zstack.header.HasThreadContext;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.log.NoLogging;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.VolumeTO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class VolumeBackupKvmCommands {
    public static final String TAKE_VOLUME_BACKUP_PATH = "/vm/volume/takebackup";
    public static final String TAKE_VOLUMES_BACKUP_PATH = "/vm/volumes/takebackup";

    public static final String CANCEL_VOLUME_BACKUP_JOBS_PATH = "/vm/volume/cancel/backupjobs";
    public static final String CANCEL_VOLUME_BACKUP_JOB_PATH = "/vm/volume/cancel/backupjob";

    public static class CancelBackupJobsCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext {
        private String vmUuid;
        private boolean force = false;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class CancelBackupJobCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext {
        private String vmUuid;
        private VolumeTO volume;
        private boolean force = false;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class CancelBackupJobsResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class CancelBackupJobResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class TakeBackupCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext, Serializable {
        private String hostname;
        private String username;
        @NoLogging
        private String password;
        private int sshPort;
        private String bsPath;
        private String uploadDir;
        private String backupPath;
        private String vmUuid;
        private VolumeTO volume;
        private String bitmap;
        private String lastBackup;
        private Long networkWriteBandwidth;
        private Long volumeWriteBandwidth;
        private Integer maxIncremental;
        private String mode;
        private ImageStoreBackupStorageCommands.StorageInfo storageInfo;
        private boolean pointInTime = true;
        private int uploadConcurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);

        public Long getNetworkWriteBandwidth() {
            return networkWriteBandwidth;
        }

        public void setNetworkWriteBandwidth(Long networkWriteBandwidth) {
            this.networkWriteBandwidth = networkWriteBandwidth;
        }

        public Long getVolumeWriteBandwidth() {
            return volumeWriteBandwidth;
        }

        public void setVolumeWriteBandwidth(Long volumeWriteBandwidth) {
            this.volumeWriteBandwidth = volumeWriteBandwidth;
        }

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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public String getBitmap() {
            return bitmap;
        }

        public void setBitmap(String bitmap) {
            this.bitmap = bitmap;
        }

        public String getLastBackup() {
            return lastBackup;
        }

        public void setLastBackup(String lastBackup) {
            this.lastBackup = lastBackup;
        }

        public Integer getMaxIncremental() {
            return maxIncremental;
        }

        public void setMaxIncremental(Integer maxIncremental) {
            this.maxIncremental = maxIncremental;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public ImageStoreBackupStorageCommands.StorageInfo getStorageInfo() {
            return storageInfo;
        }

        public void setStorageInfo(ImageStoreBackupStorageCommands.StorageInfo storageInfo) {
            this.storageInfo = storageInfo;
        }

        public String getBsPath() {
            return bsPath;
        }

        public void setBsPath(String bsPath) {
            this.bsPath = bsPath;
        }

        public String getBackupPath() {
            return backupPath;
        }

        public void setBackupPath(String backupPath) {
            this.backupPath = backupPath;
        }

        public void setPointInTime(boolean pointInTime) {
            this.pointInTime = pointInTime;
        }

        public boolean isPointInTime() {
            return pointInTime;
        }

        public void setUploadConcurrency(int uploadConcurrency) {
            this.uploadConcurrency = uploadConcurrency;
        }

        public int getUploadConcurrency() {
            return uploadConcurrency;
        }
    }

    public static class BackupInfo {
        public String bitmap;
        public Integer deviceId;
        public String lastBackup;
        public String volumeUuid;
    }

    public static class TakeBackupsCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext, Serializable {
        private String hostname;
        private String username;
        @NoLogging
        private String password;
        private int sshPort;
        private String bsPath;
        private String uploadDir;
        private List<String> backupPaths;
        private String vmUuid;
        private List<BackupInfo> backupInfos;
        private Collection<Integer> deviceIds;
        private Long networkWriteBandwidth;
        private Long volumeWriteBandwidth;
        private Integer maxIncremental;
        private String mode;
        private List<VolumeTO> volumes;
        private ImageStoreBackupStorageCommands.StorageInfo storageInfo;
        private boolean pointInTime = true;
        private int uploadConcurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);

        public Long getNetworkWriteBandwidth() {
            return networkWriteBandwidth;
        }

        public void setNetworkWriteBandwidth(Long networkWriteBandwidth) {
            this.networkWriteBandwidth = networkWriteBandwidth;
        }

        public Long getVolumeWriteBandwidth() {
            return volumeWriteBandwidth;
        }

        public void setVolumeWriteBandwidth(Long volumeWriteBandwidth) {
            this.volumeWriteBandwidth = volumeWriteBandwidth;
        }

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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<BackupInfo> getBackupInfos() {
            return backupInfos;
        }

        public void setBackupInfos(List<BackupInfo> backupInfos) {
            this.backupInfos = backupInfos;
        }

        public Collection<Integer> getDeviceIds() {
            return deviceIds;
        }

        public void setDeviceIds(Collection<Integer> deviceIds) {
            this.deviceIds = deviceIds;
        }

        public Integer getMaxIncremental() {
            return maxIncremental;
        }

        public void setMaxIncremental(Integer maxIncremental) {
            this.maxIncremental = maxIncremental;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public ImageStoreBackupStorageCommands.StorageInfo getStorageInfo() {
            return storageInfo;
        }

        public void setStorageInfo(ImageStoreBackupStorageCommands.StorageInfo storageInfo) {
            this.storageInfo = storageInfo;
        }

        public String getBsPath() {
            return bsPath;
        }

        public void setBsPath(String bsPath) {
            this.bsPath = bsPath;
        }

        public List<String> getBackupPaths() {
            return backupPaths;
        }

        public void setBackupPaths(List<String> backupPaths) {
            this.backupPaths = backupPaths;
        }

        public void setPointInTime(boolean pointInTime) {
            this.pointInTime = pointInTime;
        }

        public boolean isPointInTime() {
            return pointInTime;
        }

        public void setUploadConcurrency(int uploadConcurrency) {
            this.uploadConcurrency = uploadConcurrency;
        }

        public int getUploadConcurrency() {
            return uploadConcurrency;
        }
    }

    public static class VolumeBackupInfo {
        private int deviceId;

        @Validation
        private String backupFile;

        @Validation
        private String parentInstallPath;

        @Validation
        private String bitmap;

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public String getBackupFile() {
            return backupFile;
        }

        public void setBackupFile(String backupFile) {
            this.backupFile = backupFile;
        }

        public String getParentInstallPath() {
            return parentInstallPath;
        }

        public void setParentInstallPath(String parentInstallPath) {
            this.parentInstallPath = parentInstallPath;
        }

        public String getBitmap() {
            return bitmap;
        }

        public void setBitmap(String bitmap) {
            this.bitmap = bitmap;
        }
    }

    public static class TakeBackupsResponse extends KVMAgentCommands.AgentResponse {
        private List<VolumeBackupInfo> backupInfos;

        public List<VolumeBackupInfo> getBackupInfos() {
            return backupInfos;
        }

        public void setBackupInfos(List<VolumeBackupInfo> backupInfos) {
            this.backupInfos = backupInfos;
        }
    }

    public static class TakeBackupResponse extends KVMAgentCommands.AgentResponse {
        private String parentInstallPath;

        @Validation
        private String bitmap;

        @Validation
        private String backupFile;

        public String getParentInstallPath() {
            return parentInstallPath;
        }

        public void setParentInstallPath(String parentInstallPath) {
            this.parentInstallPath = parentInstallPath;
        }

        public String getBackupFile() {
            return backupFile;
        }

        public void setBackupFile(String backupFile) {
            this.backupFile = backupFile;
        }

        public String getBitmap() {
            return bitmap;
        }

        public void setBitmap(String bitmap) {
            this.bitmap = bitmap;
        }
    }
}
