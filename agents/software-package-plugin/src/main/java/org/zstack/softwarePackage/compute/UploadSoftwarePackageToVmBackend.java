package org.zstack.softwarePackage.compute;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.function.BooleanSupplier;

public interface UploadSoftwarePackageToVmBackend {
    String getType();

    ErrorCode validateTargetVm(VmInstanceInventory vm);

    UploadSoftwarePackageToVmSpec getUploadSpec(String uploadTaskUuid);

    void install(String vmInstanceUuid, String targetIp, String uploadTaskUuid,
                 BooleanSupplier canceled, Completion completion);
}
