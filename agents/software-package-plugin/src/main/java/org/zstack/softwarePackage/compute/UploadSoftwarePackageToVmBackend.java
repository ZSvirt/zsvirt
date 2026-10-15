package org.zstack.softwarePackage.compute;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.function.BooleanSupplier;

public interface UploadSoftwarePackageToVmBackend {
    String getType();

    ErrorCode validateTargetVm(VmInstanceInventory vm);

    UploadSoftwarePackageToVmSpec getUploadSpec(String uploadTaskUuid, long uploadTimestamp);

    ErrorCode prepareUpload(String vmInstanceUuid, String targetIp, String uploadTaskUuid,
                            long uploadTimestamp);

    ErrorCode cleanupUpload(String targetIp, long uploadTimestamp);

    default boolean isUploadCancelable(String uploadTaskUuid) {
        return true;
    }

    default void runAfterUploadFinished(String uploadTaskUuid, Runnable action) {
        action.run();
    }

    default void markUploadSettled(String uploadTaskUuid) {
    }

    void install(String vmInstanceUuid, String targetIp, String uploadTaskUuid, long uploadTimestamp,
                 BooleanSupplier canceled, Completion completion);
}
