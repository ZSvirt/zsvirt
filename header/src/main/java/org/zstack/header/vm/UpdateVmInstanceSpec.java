package org.zstack.header.vm;

/**
 * Created by Wenhao.Zhang on 23/10/16
 */
public class UpdateVmInstanceSpec {
    private String hostUuid;
    private String vmInstanceUuid;

    private String name;
    private Integer cpuNum;
    private Long memorySize;
    private Long reservedMemorySize;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public Long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    public boolean isNameChanged() {
        return name != null;
    }

    public boolean isCpuChanged() {
        return cpuNum != null;
    }

    public boolean isMemoryChanged() {
        return memorySize != null;
    }

    public boolean isReservedMemoryChanged() {
        return reservedMemorySize != null;
    }
}
