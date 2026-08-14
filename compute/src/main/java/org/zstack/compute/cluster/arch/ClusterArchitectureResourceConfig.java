package org.zstack.compute.cluster.arch;

import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/1/21 15:18
 */

public abstract class ClusterArchitectureResourceConfig {
    private Map<String, String> resourceConfigDefaultValueMap = new HashMap<>();
    private Map<String, List<String>> resourceConfigValidValuesMap = new HashMap<>();

    public void registerDefaultValue(String identity, String defaultValue) {
        resourceConfigDefaultValueMap.put(identity, defaultValue);
    }

    public void registerValidaValues(String identity, List<String> validValues) {
        resourceConfigValidValuesMap.put(identity, validValues);
    }

    public Map<String, String> getResourceConfigDefaultValueMap() {
        return resourceConfigDefaultValueMap;
    }

    public Map<String, List<String>> getResourceConfigValidValuesMap() {
        return resourceConfigValidValuesMap;
    }

    public abstract String getArchitecture();

    public Set<String> getAutoSetIfNotConfiguredClusterResourceConfigSet() {
        return resourceConfigDefaultValueMap.keySet();
    }

    public boolean clusterArchitectureMatched(String clusterUuid) {
        String architecture = Q.New(ClusterVO.class)
                .select(ClusterVO_.architecture)
                .eq(ClusterVO_.uuid, clusterUuid)
                .findValue();

        return getArchitecture().equals(architecture);
    }
}
