package org.zstack.network.l3;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class NetworkGlobalProperty {
    @GlobalProperty(name = "skip.ipv6", defaultValue = "false")
    public static boolean SKIP_IPV6;

    @GlobalProperty(name = "chssis.asset.tag", defaultValue = "www.zstack.io")
    public static String CHASSIS_ASSET_TAG;

    @GlobalProperty(name = "bridge.disable.iptables", defaultValue = "false")
    public static boolean BRIDGE_DISABLE_IPTABLES;

    @GlobalProperty(name = "bridge.disable.ip6tables", defaultValue = "false")
    public static boolean BRIDGE_DISABLE_IP6TABLES;
}
