package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Created by weiwang on 07/12/2017
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VpcDistributedRoutingGetter {
    @Autowired
    private VpcManager vpcManager;

    public Boolean getState(String vrouterUuid) {
        String enabled = vpcManager.getVpcRouterDistributedRouting(vrouterUuid);
        if (enabled != null) {
            return !enabled.equals("disabled");
        }

        /// Note(WeiW): This is hard code by vyos image
        return false;
    }
}
