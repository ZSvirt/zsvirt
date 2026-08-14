package org.zstack.header.cluster;

public interface ClusterMigrateExtensionPoint {
    void beforeMigrate(ClusterVO vo);

    void afterMigrate(ClusterVO vo);
}
