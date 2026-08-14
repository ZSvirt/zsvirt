package org.zstack.header.allocator;

import java.util.Map;

/**
 * Created by frank on 10/19/2015.
 */
public interface HostCapacityOverProvisioningManager {
    void setGlobalConfig(String category, String name);

    void setMemoryGlobalRatio(double ratio);

    double getMemoryGlobalRatio();

    void setMemoryRatio(String hostUuid, double ratio);

    void deleteMemoryRatio(String hostUuid);

    double getMemoryRatio(String hostUuid);

    Map<String, Double> getAllMemoryRatio();

    long calculateMemoryByRatio(String hostUuid, long capacity);

    long calculateHostAvailableMemoryByRatio(String hostUuid, long capacity);
}
