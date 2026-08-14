package org.zstack.scheduler;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.scheduler.SchedulerJobVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "scheduler";
    }

    {
        permissionBuilder()
                .targetResources(SchedulerJobVO.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("c2bfb343daaa4f6bbd73e3121491f154")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("scheduler")
                .build();

        apis()
                .api(
                        APIQuerySchedulerJobGroupMsg.class,
                        APIQuerySchedulerJobHistoryMsg.class,
                        APIQuerySchedulerJobMsg.class,
                        APIQuerySchedulerTriggerMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
