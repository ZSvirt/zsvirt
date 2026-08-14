package org.zstack.network.service.flat;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class FlatDhcpGlobalProperty {
    @GlobalProperty(name="upgradeFlatDhcpServerIp", defaultValue = "false")
    public static boolean UPGRADE_FLAT_DHCP_SERVER_IP;
}
