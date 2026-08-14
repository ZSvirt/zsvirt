package org.zstack.header.vmscheduling;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vm-scheduling-group";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(
                        APIAddHostToHostSchedulingRuleGroupMsg.class,
                        APICreateHostSchedulingRuleGroupMsg.class,
                        APIDetachHostFromHostSchedulingRuleGroupMsg.class,
                        APIUpdateHostSchedulingRuleGroupMsg.class,
                        APIDeleteHostSchedulingRuleGroupMsg.class
                )
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("e18d10c2c5d14885be49b1ce0cb7cb37")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("vmSchedulingRule")
                .build();

        apis()
                .api(
                        APICreateVmSchedulingRuleMsg.class,
                        APIRemoveVmSchedulingRuleMsg.class
                )
                .toService("affinityGroup")
                .build();

        apis()
                .api(
                        APIQueryHostSchedulingRuleGroupMsg.class,
                        APIQueryVmSchedulingRuleGroupMsg.class,
                        APIQueryVmSchedulingRuleMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
