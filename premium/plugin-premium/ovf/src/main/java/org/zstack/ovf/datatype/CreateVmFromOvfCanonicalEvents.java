package org.zstack.ovf.datatype;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public class CreateVmFromOvfCanonicalEvents {
    public static final String OVF_TRACK_RESULT_PATH = "/ovf/track/result";

    public static final String STAGE_READY = "ready";
    public static final String STAGE_IMAGE_UPLOAD_REQUIRED = "imageUploadRequired";
    public static final String STAGE_IMAGE_DOWNLOADING = "imageDownloading";
    public static final String STAGE_VM_CREATING = "vmCreating";
    public static final String STAGE_DONE = "done";

    public static class OvfTrackData {
        private boolean success = true;
        private ErrorCode error;
        private String stage;
        private String vmInstanceUuid;
        private VmInstanceInventory inventory;
        private List<UploadInfo> uploadInfos;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
            this.success = error == null;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public VmInstanceInventory getInventory() {
            return inventory;
        }

        public void setInventory(VmInstanceInventory inventory) {
            this.inventory = inventory;
        }

        public List<UploadInfo> getUploadInfos() {
            return uploadInfos;
        }

        public void setUploadInfos(List<UploadInfo> uploadInfos) {
            this.uploadInfos = uploadInfos;
        }
    }

    public static class UploadInfo {
        String fileName;
        String uuid;
        String installPath;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }
}
