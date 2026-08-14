package org.zstack.header.cluster;

/**
 * Created by GuoYi on 3/16/18
 */
public interface ClusterUpdateOSExtensionPoint {
    String preUpdateClusterOS(ClusterVO cls);
    void beforeUpdateClusterOS(ClusterVO cls);
    void afterUpdateClusterOS(ClusterVO cls);
}
