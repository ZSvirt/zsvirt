package org.zstack.header.cluster;

public interface MiniClusterExtensionPoint {
    void afterCreateMiniCluster(ClusterInventory cluster);
}
