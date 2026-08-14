package org.zstack.storage.primary;

import org.zstack.header.apimediator.ApiMessageInterceptionException;

public interface StorageAttachClusterMetric {
    void checkSupport(String psUuid, String clusterUuid) throws ApiMessageInterceptionException;

    String getPrimaryStorageType();

    String getClusterHypervisorType();
}
