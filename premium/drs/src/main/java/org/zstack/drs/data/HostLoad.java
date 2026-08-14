package org.zstack.drs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2019/12/17.
 */
public class HostLoad {
    private String hostUuid;

    private Float usedCPUPercent;

    private Long usedPhysicalMemoryBit;

    private List<HostLoadOccupiedByVm> vmList = new ArrayList<>();

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

    public Long getUsedPhysicalMemoryBit() {
        return usedPhysicalMemoryBit;
    }

    public void setUsedPhysicalMemoryBit(Long usedPhysicalMemoryBit) {
        this.usedPhysicalMemoryBit = usedPhysicalMemoryBit;
    }

    public List<HostLoadOccupiedByVm> getVmList() {
        return vmList;
    }

    public void setVmList(List<HostLoadOccupiedByVm> vmList) {
        this.vmList = vmList;
    }
}
