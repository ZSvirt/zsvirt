package org.zstack.ovf.api;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.volume.APICreateDataVolumeMsg;

import org.zstack.header.search.SearchConstant;
/**
 * Created by Qi Le on 2022/3/8
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "ovf";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("82a091f63581482d89e88122f39aeb96")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .actions(APICreateVmInstanceMsg.class, APICreateDataVolumeMsg.class, APIAddImageMsg.class)
                .roleName(permissionName())
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actions(
                    "org.zstack.ovf.api.**"
                )
                .build();
        apis()
                .inThisPackage()
                .toService("ovf")
                .build();

        apis()
                .api(
                        APIQueryImagePackageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
