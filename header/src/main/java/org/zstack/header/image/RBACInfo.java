package org.zstack.header.image;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "image";
    }

    {
        permissionBuilder()
                .targetResources(ImageVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("d55b63dc06b14ad1b62448fa6899729b")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIQueryImageMsg.class
                )
                .build();

        apis()
                .inThisPackage()
                .toService("image")
                .build();

        apis()
                .api(
                        APIGetCandidateBackupStorageForCreatingImageMsg.class,
                        APIGetCandidateImagesForCreatingVmMsg.class
                )
                .toService("host.allocator")
                .build();

        apis()
                .api(
                        APIQueryImageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
