package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2022/9/22 10:03
 */
public class UpdateVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private String defaultL3NetworkUuid;
    private String platform;
    private Integer cpuNum;
    private Long memorySize;
    private Long reservedMemorySize;
    private String guestOsType;
    private String allocatorStrategy;
    private Boolean vmEncryption;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public Boolean getVmEncryption() {
        return vmEncryption;
    }

    public void setVmEncryption(Boolean vmEncryption) {
        this.vmEncryption = vmEncryption;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}
