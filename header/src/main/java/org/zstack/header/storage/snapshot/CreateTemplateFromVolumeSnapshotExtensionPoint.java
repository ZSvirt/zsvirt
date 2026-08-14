package org.zstack.header.storage.snapshot;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.image.ImageInventory;

/**
 * Created by xing5 on 2016/4/29.
 */
public interface CreateTemplateFromVolumeSnapshotExtensionPoint {
    class WorkflowTemplate {
        private Flow createTemporaryTemplate;
        private Flow uploadToBackupStorage;
        private Flow deleteTemporaryTemplate;

        public Flow getCreateTemporaryTemplate() {
            return createTemporaryTemplate;
        }

        public void setCreateTemporaryTemplate(Flow createTemporaryTemplate) {
            this.createTemporaryTemplate = createTemporaryTemplate;
        }

        public Flow getUploadToBackupStorage() {
            return uploadToBackupStorage;
        }

        public void setUploadToBackupStorage(Flow uploadToBackupStorage) {
            this.uploadToBackupStorage = uploadToBackupStorage;
        }

        public Flow getDeleteTemporaryTemplate() {
            return deleteTemporaryTemplate;
        }

        public void setDeleteTemporaryTemplate(Flow deleteTemporaryTemplate) {
            this.deleteTemporaryTemplate = deleteTemporaryTemplate;
        }
    }

    class ParamIn {
        private VolumeSnapshotInventory snapshot;
        private ImageInventory image;
        private String primaryStorageUuid;
        private String backupStorageUuid;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public VolumeSnapshotInventory getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(VolumeSnapshotInventory snapshot) {
            this.snapshot = snapshot;
        }

        public ImageInventory getImage() {
            return image;
        }

        public void setImage(ImageInventory image) {
            this.image = image;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }
    }

    class ParamOut {
        private long actualSize;
        private long size;
        private String backupStorageInstallPath;

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

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


    WorkflowTemplate createTemplateFromVolumeSnapshot(ParamIn paramIn);

    String createTemplateFromVolumeSnapshotPrimaryStorageType();
}
