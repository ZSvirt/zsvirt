package org.zstack.header.storageDevice;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "scsi-lun";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryScsiLunMsg.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(APIQueryScsiLunMsg.class)
                .build();
        apis()
                .inThisPackage()
                .toService("storageDevice")
                .build();

        apis()
                .api(
                        APIQueryScsiLunMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
