package org.zstack.monitoring;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.monitoring.actions.APIQueryEmailTriggerActionMsg;
import org.zstack.monitoring.actions.APIQueryMonitorTriggerActionMsg;
import org.zstack.monitoring.media.APIQueryEmailMediaMsg;
import org.zstack.monitoring.media.APIQueryMediaMsg;
/**
 * Created by kayo on 2018/7/10.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "monitoring";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .api(
                        APIAttachMonitorTriggerActionToTriggerMsg.class,
                        APIChangeMonitorTriggerStateMsg.class,
                        APICreateMonitorTriggerMsg.class,
                        APIDeleteAlertMsg.class,
                        APIDeleteMonitorTriggerMsg.class,
                        APIDetachMonitorTriggerActionFromTriggerMsg.class,
                        APIGetMonitorItemMsg.class,
                        APIUpdateMonitorTriggerMsg.class
                )
                .toService("monitoring")
                .build();

        apis()
                .api(
                        APIQueryAlertMsg.class,
                        APIQueryMonitorTriggerMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.monitoring.actions")
                .toService("monitoring")
                .build();
        apis()
                .api(
                        APIQueryEmailTriggerActionMsg.class,
                        APIQueryMonitorTriggerActionMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.monitoring.media")
                .toService("media")
                .build();
        apis()
                .api(
                        APIQueryEmailMediaMsg.class,
                        APIQueryMediaMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.monitoring.prometheus")
                .toService("prometheus.legacy")
                .build();
    }
}
