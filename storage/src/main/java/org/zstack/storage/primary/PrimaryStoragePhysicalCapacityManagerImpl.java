package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 11/9/2015.
 */
public class PrimaryStoragePhysicalCapacityManagerImpl implements PrimaryStoragePhysicalCapacityManager {
    private double globalRatio = 1;
    private ConcurrentHashMap<String, Double> primaryStorageRatio = new ConcurrentHashMap<String, Double>();
    private GlobalConfig globalConfig;

    @Autowired
    GlobalConfigFacade gcf;

    @Autowired
    ResourceConfigFacade rcf;

    @Override
    public void setGlobalConfig(String category, String name) {
        globalConfig = gcf.getAllConfig().get(GlobalConfig.produceIdentity(category, name));
        globalRatio = globalConfig.value(Double.class);
    }

    @Override
    public void setGlobalRatio(double ratio) {
        globalRatio = ratio;
    }

    @Override
    public double getGlobalRatio() {
        return globalRatio;
    }

    @Override
    public void setRatio(String psUuid, double ratio) {
        primaryStorageRatio.put(psUuid, ratio);
    }

    @Override
    public void deleteRatio(String psUuid) {
        primaryStorageRatio.remove(psUuid);
    }

    @Override
    public double getRatio(String psUuid) {
        Double ratio = primaryStorageRatio.get(psUuid);
        if (ratio != null) {
            return ratio;
        }

        if (globalConfig != null) {
            return rcf.getResourceConfigValue(globalConfig, psUuid, Double.class);
        } else {
            return globalRatio;
        }
    }

    @Override
    public Map<String, Double> getAllRatio() {
        return primaryStorageRatio;
    }

    @Override
    public boolean checkCapacityByRatio(String psUuid, long totalPhysicalCapacity, long availablePhysicalCapacity) {
        if (availablePhysicalCapacity == totalPhysicalCapacity) {
            return true;
        }

        double ratio = getRatio(psUuid);
        long used = totalPhysicalCapacity - availablePhysicalCapacity;
        return used < Math.round(totalPhysicalCapacity * ratio);
    }

    @Override
    public boolean checkRequiredCapacityByRatio(String psUuid, long totalPhysicalCapacity, long requiredCapacity) {
        double ratio = getRatio(psUuid);
        return requiredCapacity < Math.round(totalPhysicalCapacity * ratio);
    }

    @Override
    public boolean checkCapacityByRatio(String psUuid, PrimaryStorageCapacityVO cap) {
        return checkCapacityByRatio(psUuid, cap.getTotalPhysicalCapacity(), cap.getAvailablePhysicalCapacity());
    }
}
