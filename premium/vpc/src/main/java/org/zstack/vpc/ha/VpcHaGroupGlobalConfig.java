package org.zstack.vpc.ha;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vpc.ha.VpcHaGroupVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class VpcHaGroupGlobalConfig {
    public static final String CATEGORY = "vpcHa";

    @GlobalConfigValidation(inNumberRange = {1, 60})
    @BindResourceConfig({VpcHaGroupVO.class})
    public static GlobalConfig KEEPALIVED_INTERVAL = new GlobalConfig(CATEGORY, "keepalived.interval");

    @GlobalConfigValidation
    @BindResourceConfig({VpcHaGroupVO.class})
    public static GlobalConfig CONFIG_FIREWALL_WITH_IPTABLES = new GlobalConfig(CATEGORY, "configure.firewall.with.iptables");

    @GlobalConfigValidation
    @BindResourceConfig({VpcHaGroupVO.class})
    public static GlobalConfig TC_FOR_VIPQOS = new GlobalConfig(CATEGORY, "tc.for.vipqos");
}
