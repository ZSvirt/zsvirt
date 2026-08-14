package org.zstack.network.service.lb;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class LoadBalancerGlobalProperty {
    @GlobalProperty(name="upgradeLoadBalancerServerGroup", defaultValue = "false")
    public static boolean UPGRADE_LB_SERVER_GROUP;
}
