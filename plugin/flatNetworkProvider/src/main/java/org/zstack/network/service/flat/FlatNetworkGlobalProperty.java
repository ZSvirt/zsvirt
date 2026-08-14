package org.zstack.network.service.flat;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/6/25.
 */
@GlobalPropertyDefinition
public class FlatNetworkGlobalProperty {
    @GlobalProperty(name="deleteDeprecatedFlatDHCPNameSpace", defaultValue = "false")
    public static boolean DELETE_DEPRECATED_DHCP_NAME_SPACE;
}
