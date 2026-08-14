package org.zstack.portal.managementnode;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/7/22.
 */
@GlobalPropertyDefinition
public class PortalGlobalProperty {
    @GlobalProperty(name = "ManagementNode.maxHeartbeatFailure", defaultValue = "5")
    public static int MAX_HEARTBEAT_FAILURE;
    @GlobalProperty(name = "ManagementNode.heartbeat.jdbcNetworkTimeout", defaultValue = "5")
    public static int HEART_BEAT_QUERY_TIMEOUT;
}
