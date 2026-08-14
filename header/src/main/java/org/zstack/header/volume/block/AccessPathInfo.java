package org.zstack.header.volume.block;

import java.util.List;

/**
 * @author shenjin
 * @date 2023/6/21 13:27
 */
public class AccessPathInfo {
    private String name;

    private Integer accessPathId;

    private String accessPathIqn;
    
    private Integer targetCount;

    private List<String> gatewayIps;

    public List<String> getGatewayIps() {
        return gatewayIps;
    }

    public void setGatewayIps(List<String> gatewayIps) {
        this.gatewayIps = gatewayIps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAccessPathId() {
        return accessPathId;
    }

    public void setAccessPathId(Integer accessPathId) {
        this.accessPathId = accessPathId;
    }

    public String getAccessPathIqn() {
        return accessPathIqn;
    }

    public void setAccessPathIqn(String accessPathIqn) {
        this.accessPathIqn = accessPathIqn;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }
}

