package org.zstack.header.cluster;


public interface ClusterDeleteExtensionPoint {
    void preDeleteCluster(ClusterInventory inventory) throws ClusterException;

    void beforeDeleteCluster(ClusterInventory inventory);

    void afterDeleteCluster(ClusterInventory inventory);
}
