package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class VirtualSwitchGlobalProperty {
    @GlobalProperty(name="upgradeL2VirtualSwitchUplinkBonding", defaultValue = "false")
    public static boolean UPGRADE_L2_VIRTUAL_SWITCH_UPLINK_BONDING;

    @GlobalProperty(name="upgradePortGroup", defaultValue = "false")
    public static boolean UPGRADE_PORT_GROUP;
}
