package org.zstack.header.allocator;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageMsg;
import org.zstack.header.image.APIGetCandidateImagesForCreatingVmMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "host-allocator";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIGetCpuMemoryCapacityMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();

        apis()
                .inThisPackage()
                .toService(HostAllocatorConstant.SERVICE_ID)
                .build();
    }
}
