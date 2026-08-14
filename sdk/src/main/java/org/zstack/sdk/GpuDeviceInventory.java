package org.zstack.sdk;



public class GpuDeviceInventory extends org.zstack.sdk.PciDeviceInventory {

    public java.lang.String serialNumber;
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public java.lang.String getSerialNumber() {
        return this.serialNumber;
    }

    public java.lang.Long memory;
    public void setMemory(java.lang.Long memory) {
        this.memory = memory;
    }
    public java.lang.Long getMemory() {
        return this.memory;
    }

    public java.lang.Long power;
    public void setPower(java.lang.Long power) {
        this.power = power;
    }
    public java.lang.Long getPower() {
        return this.power;
    }

    public java.lang.Boolean isDriverLoaded;
    public void setIsDriverLoaded(java.lang.Boolean isDriverLoaded) {
        this.isDriverLoaded = isDriverLoaded;
    }
    public java.lang.Boolean getIsDriverLoaded() {
        return this.isDriverLoaded;
    }

}
