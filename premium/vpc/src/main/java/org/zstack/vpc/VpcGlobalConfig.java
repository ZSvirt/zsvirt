package org.zstack.vpc;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by weiwang on 29/11/2017
 */
@GlobalConfigDefinition
public class VpcGlobalConfig {
    public static final String CATEGORY = "vpc";

    @GlobalConfigValidation(min = 30)
    public static GlobalConfig ZSNP_TMOUT = new GlobalConfig(CATEGORY, "zsnp.timeout");

    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig CONFIG_FIREWALL_WITH_IPTABLES = new GlobalConfig(CATEGORY, "configure.firewall.with.iptables");

    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig TC_FOR_VIPQOS = new GlobalConfig(CATEGORY, "tc.for.vipqos");
}
