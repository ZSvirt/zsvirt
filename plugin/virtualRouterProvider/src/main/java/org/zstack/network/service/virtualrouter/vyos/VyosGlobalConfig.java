package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.service.lb.LoadBalancerVO;

/**
 * Created by shixin.ruan on 18/03/09.
 */
@GlobalConfigDefinition
public class VyosGlobalConfig {
    public static final String CATEGORY = "vyos";

    @GlobalConfigValidation(validValues = {"accept", "reject"})
    public static GlobalConfig PRIVATE_L3_FIREWALL_DEFAULT_ACTION = new GlobalConfig(CATEGORY, "private.l3.firewall.default.action");

    @GlobalConfigValidation
    @BindResourceConfig({L3NetworkVO.class})
    public static GlobalConfig CONFIG_FIREWALL_WITH_IPTABLES = new GlobalConfig(CATEGORY, "configure.firewall.with.iptables");

    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig AUTO_RESTART_IPSEC = new GlobalConfig(CATEGORY, "auto.restart.ipsec");

    @GlobalConfigValidation
    @BindResourceConfig({LoadBalancerVO.class})
    public static GlobalConfig ENABLE_HAPROXY_LOG = new GlobalConfig(CATEGORY, "enable.haproxy.log");

    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig ENABLE_VYOS_CMD = new GlobalConfig(CATEGORY, "enable.vyos.cmd");
}
