package org.zstack.network.service.eip;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class EipGlobalConfig {
    public static final String CATEGORY = "eip";

    @GlobalConfigValidation
    public static GlobalConfig SNAT_INBOUND_TRAFFIC = new GlobalConfig(CATEGORY, "snatInboundTraffic");
}
