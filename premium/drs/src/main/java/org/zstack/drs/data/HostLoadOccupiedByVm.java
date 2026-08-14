package org.zstack.drs.data;

/**
 * Created by lining on 2019/12/17.
 */
public class HostLoadOccupiedByVm {
    private String vmUuid;

    private Float usedHostCPUPercent;

    private Long usedHostPhysicalMemoryBit;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public Float getUsedHostCPUPercent() {
        return usedHostCPUPercent;
    }

    public void setUsedHostCPUPercent(Float usedHostCPUPercent) {
        this.usedHostCPUPercent = usedHostCPUPercent;
    }

    public Long getUsedHostPhysicalMemoryBit() {
        return usedHostPhysicalMemoryBit;
    }

    public void setUsedHostPhysicalMemoryBit(Long usedHostPhysicalMemoryBit) {
        this.usedHostPhysicalMemoryBit = usedHostPhysicalMemoryBit;
    }
}
