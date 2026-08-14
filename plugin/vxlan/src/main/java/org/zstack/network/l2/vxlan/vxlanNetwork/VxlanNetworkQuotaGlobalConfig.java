package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */

@GlobalConfigDefinition
public class VxlanNetworkQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VXLAN_NUM = new GlobalConfig(CATEGORY, VxlanNetworkQuotaConstant.VXLAN_NUM);
}
