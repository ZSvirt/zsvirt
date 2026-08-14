package org.zstack.guesttools;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;

import org.zstack.header.search.SearchConstant;
@SDKPackage(packageName = "org.zstack.sdk.guesttools")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "guest-tools";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .api(
                        APIAttachGuestToolsIsoToVmMsg.class,
                        APIDetachGuestToolsIsoFromVmMsg.class,
                        APIGetLatestGuestToolsForVmMsg.class,
                        APIGetVmGuestToolsInfoMsg.class,
                        APIUpdateGuestToolsStateMsg.class,
                        APIUpdateVmNetworkConfigMsg.class
                )
                .toService("guest.tools")
                .build();

        apis()
                .api(
                        APIQueryGuestToolsStateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
