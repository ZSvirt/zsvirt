package org.zstack.softwarePackage.compute;

import org.zstack.header.message.CancelTaskResult;

final class UploadSoftwarePackageToVmCancellationPolicy {
    private UploadSoftwarePackageToVmCancellationPolicy() {
    }

    static boolean isCompleted(CancelTaskResult result, boolean uploadCancelable) {
        return uploadCancelable && result == CancelTaskResult.TASK_NOT_FOUND;
    }
}
