package org.zstack.network.service.portforwarding;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class PortForwardingGlobalConfig {
    public static final String CATEGORY = "portForwarding";

    @GlobalConfigValidation
    public static GlobalConfig SNAT_INBOUND_TRAFFIC = new GlobalConfig(CATEGORY, "snatInboundTraffic");
}
