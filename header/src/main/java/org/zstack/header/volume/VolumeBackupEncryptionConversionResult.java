package org.zstack.header.volume;

import java.util.ArrayList;
import java.util.List;

public class VolumeBackupEncryptionConversionResult {
    public static class RefRepoint {
        private long refId;
        private String backupUuid;
        private String backupStorageUuid;
        private String oldInstallPath;
        private String newInstallPath;
        private boolean sourceEncrypted;

        public long getRefId() {
            return refId;
        }

        public void setRefId(long refId) {
            this.refId = refId;
        }

        public String getBackupUuid() {
            return backupUuid;
        }

        public void setBackupUuid(String backupUuid) {
            this.backupUuid = backupUuid;
        }

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getOldInstallPath() {
            return oldInstallPath;
        }

        public void setOldInstallPath(String oldInstallPath) {
            this.oldInstallPath = oldInstallPath;
        }

        public String getNewInstallPath() {
            return newInstallPath;
        }

        public void setNewInstallPath(String newInstallPath) {
            this.newInstallPath = newInstallPath;
        }

        public boolean isSourceEncrypted() {
            return sourceEncrypted;
        }

        public void setSourceEncrypted(boolean sourceEncrypted) {
            this.sourceEncrypted = sourceEncrypted;
        }
    }

    public static class DeleteBits {
        private String backupStorageUuid;
        private String installPath;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    private String volumeUuid;
    private boolean targetEncrypted;
    private List<RefRepoint> refsToRepoint = new ArrayList<>();
    private List<String> convertedBackupUuids = new ArrayList<>();
    private List<String> placeholderBackupUuids = new ArrayList<>();
    private List<DeleteBits> deleteOldBits = new ArrayList<>();

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isTargetEncrypted() {
        return targetEncrypted;
    }

    public void setTargetEncrypted(boolean targetEncrypted) {
        this.targetEncrypted = targetEncrypted;
    }

    public List<RefRepoint> getRefsToRepoint() {
        return refsToRepoint;
    }

    public void setRefsToRepoint(List<RefRepoint> refsToRepoint) {
        this.refsToRepoint = refsToRepoint;
    }

    public List<String> getConvertedBackupUuids() {
        return convertedBackupUuids;
    }

    public void setConvertedBackupUuids(List<String> convertedBackupUuids) {
        this.convertedBackupUuids = convertedBackupUuids;
    }

    public List<String> getPlaceholderBackupUuids() {
        return placeholderBackupUuids;
    }

    public void setPlaceholderBackupUuids(List<String> placeholderBackupUuids) {
        this.placeholderBackupUuids = placeholderBackupUuids;
    }

    public List<DeleteBits> getDeleteOldBits() {
        return deleteOldBits;
    }

    public void setDeleteOldBits(List<DeleteBits> deleteOldBits) {
        this.deleteOldBits = deleteOldBits;
    }
}
