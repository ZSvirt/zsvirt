package org.zstack.drs.data;

/**
 * Created by lining on 2019/12/17.
 */
public class HostLoadOccupiedByVm {
    private String vmUuid;

    private Float usedHostPhysicalCpuNum; // Occupies the CPU cores of the physical machine from top command

    private Long usedHostPhysicalMemoryBit;

    public HostLoadOccupiedByVm() {}

    public HostLoadOccupiedByVm(String vmUuid, Float usedHostPhysicalCpuNum, Long usedHostPhysicalMemoryBit) {
        this.vmUuid = vmUuid;
        this.usedHostPhysicalCpuNum = usedHostPhysicalCpuNum;
        this.usedHostPhysicalMemoryBit = usedHostPhysicalMemoryBit;
    }


    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public Float getUsedHostPhysicalCpuNum() {
        return usedHostPhysicalCpuNum;
    }

    public void setUsedHostPhysicalCpuNum(Float usedHostPhysicalCpuNum) {
        this.usedHostPhysicalCpuNum = usedHostPhysicalCpuNum;
    }

    public Long getUsedHostPhysicalMemoryBit() {
        return usedHostPhysicalMemoryBit;
    }

    public void setUsedHostPhysicalMemoryBit(Long usedHostPhysicalMemoryBit) {
        this.usedHostPhysicalMemoryBit = usedHostPhysicalMemoryBit;
    }
}
