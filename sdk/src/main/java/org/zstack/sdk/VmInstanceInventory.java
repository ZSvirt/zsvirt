package org.zstack.sdk;



public class VmInstanceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

    public java.lang.String clusterUuid;
    public void setClusterUuid(java.lang.String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
    public java.lang.String getClusterUuid() {
        return this.clusterUuid;
    }

    public java.lang.String imageUuid;
    public void setImageUuid(java.lang.String imageUuid) {
        this.imageUuid = imageUuid;
    }
    public java.lang.String getImageUuid() {
        return this.imageUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String lastHostUuid;
    public void setLastHostUuid(java.lang.String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }
    public java.lang.String getLastHostUuid() {
        return this.lastHostUuid;
    }

    public java.lang.String instanceOfferingUuid;
    public void setInstanceOfferingUuid(java.lang.String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }
    public java.lang.String getInstanceOfferingUuid() {
        return this.instanceOfferingUuid;
    }

    public java.lang.String rootVolumeUuid;
    public void setRootVolumeUuid(java.lang.String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }
    public java.lang.String getRootVolumeUuid() {
        return this.rootVolumeUuid;
    }

    public java.lang.String platform;
    public void setPlatform(java.lang.String platform) {
        this.platform = platform;
    }
    public java.lang.String getPlatform() {
        return this.platform;
    }

    public java.lang.String architecture;
    public void setArchitecture(java.lang.String architecture) {
        this.architecture = architecture;
    }
    public java.lang.String getArchitecture() {
        return this.architecture;
    }

    public java.lang.String defaultL3NetworkUuid;
    public void setDefaultL3NetworkUuid(java.lang.String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }
    public java.lang.String getDefaultL3NetworkUuid() {
        return this.defaultL3NetworkUuid;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String hypervisorType;
    public void setHypervisorType(java.lang.String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public java.lang.String getHypervisorType() {
        return this.hypervisorType;
    }

    public java.lang.Long memorySize;
    public void setMemorySize(java.lang.Long memorySize) {
        this.memorySize = memorySize;
    }
    public java.lang.Long getMemorySize() {
        return this.memorySize;
    }

    public java.lang.Long reservedMemorySize;
    public void setReservedMemorySize(java.lang.Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }
    public java.lang.Long getReservedMemorySize() {
        return this.reservedMemorySize;
    }

    public java.lang.Integer cpuNum;
    public void setCpuNum(java.lang.Integer cpuNum) {
        this.cpuNum = cpuNum;
    }
    public java.lang.Integer getCpuNum() {
        return this.cpuNum;
    }

    public java.lang.Long cpuSpeed;
    public void setCpuSpeed(java.lang.Long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }
    public java.lang.Long getCpuSpeed() {
        return this.cpuSpeed;
    }

    public java.lang.String allocatorStrategy;
    public void setAllocatorStrategy(java.lang.String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }
    public java.lang.String getAllocatorStrategy() {
        return this.allocatorStrategy;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.util.List vmNics;
    public void setVmNics(java.util.List vmNics) {
        this.vmNics = vmNics;
    }
    public java.util.List getVmNics() {
        return this.vmNics;
    }

    public java.util.List allVolumes;
    public void setAllVolumes(java.util.List allVolumes) {
        this.allVolumes = allVolumes;
    }
    public java.util.List getAllVolumes() {
        return this.allVolumes;
    }

    public java.util.List vmCdRoms;
    public void setVmCdRoms(java.util.List vmCdRoms) {
        this.vmCdRoms = vmCdRoms;
    }
    public java.util.List getVmCdRoms() {
        return this.vmCdRoms;
    }

    public java.lang.String guestOsType;
    public void setGuestOsType(java.lang.String guestOsType) {
        this.guestOsType = guestOsType;
    }
    public java.lang.String getGuestOsType() {
        return this.guestOsType;
    }

}
