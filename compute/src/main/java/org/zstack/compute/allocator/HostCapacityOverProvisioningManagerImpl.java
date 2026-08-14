package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 10/19/2015.
 */
public class HostCapacityOverProvisioningManagerImpl implements HostCapacityOverProvisioningManager {
    private double globalMemoryRatio = 1;
    private ConcurrentHashMap<String, Double> hostMemoryRatio = new ConcurrentHashMap<String, Double>();
    private GlobalConfig globalConfig;

    @Autowired
    GlobalConfigFacade gcf;

    @Autowired
    ResourceConfigFacade rcf;

    @Override
    public void setGlobalConfig(String category, String name) {
        globalConfig = gcf.getAllConfig().get(GlobalConfig.produceIdentity(category, name));
        globalMemoryRatio = globalConfig.value(Double.class);
    }

    @Override
    public void setMemoryGlobalRatio(double ratio) {
        globalMemoryRatio = ratio;
    }

    @Override
    public double getMemoryGlobalRatio() {
        return globalMemoryRatio;
    }

    @Override
    public void setMemoryRatio(String hostUuid, double ratio) {
        hostMemoryRatio.put(hostUuid, ratio);
    }

    @Override
    public void deleteMemoryRatio(String hostUuid) {
        hostMemoryRatio.remove(hostUuid);
    }

    @Override
    public double getMemoryRatio(String hostUuid) {
        Double ratio =  hostMemoryRatio.get(hostUuid);
        if (ratio != null) {
            return ratio;
        }
        if (globalConfig != null) {
            return rcf.getResourceConfigValue(globalConfig, hostUuid, Double.class);
        } else {
            return globalMemoryRatio;
        }
    }

    @Override
    public Map<String, Double> getAllMemoryRatio() {
        return hostMemoryRatio;
    }

    @Override
    public long calculateMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity / ratio);
    }

    @Override
    public long calculateHostAvailableMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity * ratio);
    }
}
