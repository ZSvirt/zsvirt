package org.zstack.core.debug;

import org.zstack.header.core.APIGetChainTaskMsg;
import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-debug";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(
                        APIDebugSignalMsg.class,
                        APIGetDebugSignalMsg.class,
                        APICleanQueueMsg.class,
                        APIGetChainTaskMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inThisPackage()
                .toService("debug")
                .build();
    }
}
