package org.zstack.drs.api;

import org.zstack.header.rest.SDK;

/**
 * Created by lining on 2019/12/13.
 */
@SDK
public class HostLoad {
    private String hostUuid;
    private Float usedCPUPercent;
    private Float usedMemoryPercent;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Float getUsedCPUPercent() {
        return usedCPUPercent;
    }

    public void setUsedCPUPercent(Float usedCPUPercent) {
        this.usedCPUPercent = usedCPUPercent;
    }

    public Float getUsedMemoryPercent() {
        return usedMemoryPercent;
    }

    public void setUsedMemoryPercent(Float usedMemoryPercent) {
        this.usedMemoryPercent = usedMemoryPercent;
    }
}
