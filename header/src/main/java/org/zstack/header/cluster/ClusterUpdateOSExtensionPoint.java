package org.zstack.header.cluster;

/**
 * Created by GuoYi on 3/16/18
 */
public interface ClusterUpdateOSExtensionPoint {
    // TODO it is better to return ErrorCode
    String preUpdateClusterOS(ClusterVO cls);
    void beforeUpdateClusterOS(ClusterVO cls);
    void afterUpdateClusterOS(ClusterVO cls);
}
