package org.zstack.portal.managementnode;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ManagementNodeGlobalConfig {
    public static final String CATEGORY = "managementServer";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig NODE_HEARTBEAT_INTERVAL = new GlobalConfig(CATEGORY, "node.heartbeatInterval");
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig NODE_JOIN_DELAY = new GlobalConfig(CATEGORY, "node.joinDelay");
}
