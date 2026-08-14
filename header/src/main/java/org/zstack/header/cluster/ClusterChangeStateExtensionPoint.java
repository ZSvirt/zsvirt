package org.zstack.header.cluster;


public interface ClusterChangeStateExtensionPoint {
    void preChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) throws ClusterException;

    void beforeChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState);

    void afterChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState previousState);
}
