package org.zstack.header.allocator;

import java.util.Map;

/**
 * Created by xing5 on 2016/5/12.
 */
public interface HostCpuOverProvisioningManager {
    void setGlobalRatio(int ratio);

    int getGlobalRatio();

    void setRatio(String hostUuid, int ratio);

    void deleteRatio(String hostUuid);

    int getRatio(String hostUuid);

    Map<String, Integer> getAllRatio();

    int calculateByRatio(String hostUuid, int cpuNum);

    int calculateHostCpuByRatio(String hostUuid, int cpuNum);
}
