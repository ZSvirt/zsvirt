package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * @author: zhanyong.miao
 * @date: 2019-04-24
 **/
@GlobalConfigDefinition
public class VxlanNetworkGlobalConfig {
    public static final String CATEGORY = "vxlan";

    @GlobalConfigValidation
    public static GlobalConfig CLUSTER_LAZY_ATTACH = new GlobalConfig(CATEGORY, "cluster.lazyAttach");
}