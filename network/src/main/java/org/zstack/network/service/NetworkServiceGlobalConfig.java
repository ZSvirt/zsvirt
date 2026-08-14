package org.zstack.network.service;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by weiwang on 13/05/2017.
 */
@GlobalConfigDefinition
public class NetworkServiceGlobalConfig {
    public static final String CATEGORY = "networkService";

    @GlobalConfigValidation
    @BindResourceConfig({L2NetworkVO.class})
    public static GlobalConfig DHCP_MTU_NO_VLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2NoVlanNetwork");

    @GlobalConfigValidation
    @BindResourceConfig({L2NetworkVO.class})
    public static GlobalConfig DHCP_MTU_VLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2VlanNetwork");

    @GlobalConfigValidation
    @BindResourceConfig({L2NetworkVO.class})
    public static GlobalConfig DHCP_MTU_VXLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2VxlanNetwork");

    @GlobalConfigValidation
    public static GlobalConfig DHCP_MTU_DUMMY = new GlobalConfig(CATEGORY, "defaultDhcpMtu.dummyNetwork");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_VHOSTUSER = new GlobalConfig(CATEGORY, "enableVHostUser");
}
