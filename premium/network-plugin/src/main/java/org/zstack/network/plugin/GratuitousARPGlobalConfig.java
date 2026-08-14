package org.zstack.network.plugin;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class GratuitousARPGlobalConfig {
    public static final String CATEGORY = "networkPlugin";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig SEND_GRATUITOUS_ARP_INTERVAL = new PremiumGlobalConfig(CATEGORY, "gratuitousARP.interval");

}
