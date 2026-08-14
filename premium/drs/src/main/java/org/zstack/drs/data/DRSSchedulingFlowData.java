package org.zstack.drs.data;

import java.io.Serializable;
import java.util.List;

public class DRSSchedulingFlowData implements Serializable {
    private List<String> hostUuids;

    private Boolean hostLoadOverThreshold;

    private List<HostLoad> hostLoads;

    public Boolean getHostLoadOverThreshold() {
        return hostLoadOverThreshold;
    }

    public void setHostLoadOverThreshold(Boolean hostLoadOverThreshold) {
        this.hostLoadOverThreshold = hostLoadOverThreshold;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public List<HostLoad> getHostLoads() {
        return hostLoads;
    }

    public void setHostLoads(List<HostLoad> hostLoads) {
        this.hostLoads = hostLoads;
    }
}
