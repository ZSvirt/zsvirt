package org.zstack.header.storage.primary;

import java.util.Map;

/**
 * Created by frank on 10/19/2015.
 */
public interface PrimaryStorageOverProvisioningManager {
    void setGlobalConfig(String category, String name);

    void setGlobalRatio(double ratio);

    double getGlobalRatio();

    void setRatio(String psUuid, double ratio);

    void deleteRatio(String psUuid);

    double getRatio(String psUuid);

    Map<String, Double> getAllRatio();

    long calculateByRatio(String psUuid, long capacity);

    long calculatePrimaryStorageAvailableCapacityByRatio(String psUuid, long capacity);
}
