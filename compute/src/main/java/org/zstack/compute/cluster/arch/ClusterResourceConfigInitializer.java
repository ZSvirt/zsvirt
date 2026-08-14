package org.zstack.compute.cluster.arch;

import org.zstack.header.cluster.ClusterInventory;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/2/7 14:20
 */
public interface ClusterResourceConfigInitializer {
    public void initClusterResourceConfigValue(ClusterInventory cluster);
}
