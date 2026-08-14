package org.zstack.softwarePackage.compute;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.softwarePackage.header.SoftwarePackageInventory;

public class SoftwarePackageCanonicalEvents {
    public static final String SOFTWARE_PACKAGE_TRACK_RESULT_PATH = "/software-package/track/result";
    public static final String SOFTWARE_PACKAGE_TO_VM_TRACK_RESULT_PATH =
            "/software-package/vm-upload/track/result";

    @NeedJsonSchema
    public static class SoftwarePackageTrackData {
        public boolean success = true;
        public ErrorCode error;
        public String uuid;
        public SoftwarePackageInventory inventory;

        public void setError(ErrorCode error) {
            this.error = error;
            this.success = error == null;
        }
    }

    @NeedJsonSchema
    public static class SoftwarePackageToVmTrackData {
        private String uploadTaskUuid;
        private ErrorCode error;

        public String getUploadTaskUuid() {
            return uploadTaskUuid;
        }

        public void setUploadTaskUuid(String uploadTaskUuid) {
            this.uploadTaskUuid = uploadTaskUuid;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }
    }
}
