package org.zstack.header.storage.backup;

import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeType;

import java.util.List;
import java.util.function.Supplier;

public class VolumeMetadata {
    /**
     * 'type' can be either 'Root' or 'Data'
     */
    private String type;

    /**
     * Name of the volume by the time backup was created
     */
    private String name;

    /**
     * Description of the volume by the time backup was created
     */
    private String description;

    /**
     * The name of corresponding VM
     */
    private String vmName;

    /**
     * The description of corresponding VM
     */
    private String vmDescription;

    /**
     * The VM instance uuid
     */
    private String vmInstanceUuid;

    /**
     * The systemTags of corresponding VM
     */
    private List<String> vmSystemTags;

    /**
     * The platform of the VM
     */
    private String platform;

    /**
     * The guestOsType of the VM
     */
    private String guestOsType;

    /**
     * The driver of the VM
     */
    private boolean virtio;

    /**
     * The architecture of the VM
     */
    private String architecture;

    /**
     * The cluster UUID
     */
    private String clusterUuid;

    /**
     * The primary storage uuid
     */
    private String primaryStorageUuid;

    private Integer deviceId;

    private String rootImageUuid;

    /**
     * The CPU number
     */
    private int cpuNum;

    /**
     * The memory size in byte.
     */
    private long memorySize;

    /**
     * The instance offering uuid.
     */
    private String instanceOfferingUuid;

    /**
     * The default L3 network uuid for the original VM
     */
    private String defaultL3NetworkUuid;

    /**
     * The NICs of the VM
     */
    private List<VmNicInventory> vmNics;

    /**
     * Data volume uuids for whole-vm backup
     */
    private List<String> dataVolumeUuids;

    /**
     * Volume virtual size
     */
    private long size;

    /**
     * Volume actual size
     */
    private long actualSize;

    /**
     * Volume format
     */
    private String format;

    /**
     * Is shareable volume
     */
    private boolean isShareable;

    private List<SystemTagInventory> systemTags;

    /**
     * Volume attached vm
     */
    private String attachedVmUuid;

    /**
     * Volume owner account
     */
    private String ownerAccountUuid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmDescription() {
        return vmDescription;
    }

    public void setVmDescription(String vmDescription) {
        this.vmDescription = vmDescription;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPlatformOrElse(Supplier<String> platformGetter) {
        return platform != null ? platform : platformGetter.get();
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getArchitectureOrElse(Supplier<String> archGetter) {
        if (architecture != null) {
            return architecture;
        } else if (VolumeType.Root.toString().equals(type)) {
            return archGetter.get();
        } else {
            return null;
        }
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public List<VmNicInventory> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicInventory> vmNics) {
        this.vmNics = vmNics;
    }

    public List<String> getDataVolumeUuids() {
        return dataVolumeUuids;
    }

    public void setDataVolumeUuids(List<String> dataVolumeUuids) {
        this.dataVolumeUuids = dataVolumeUuids;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isShareable() {
        return isShareable;
    }

    public void setShareable(boolean shareable) {
        isShareable = shareable;
    }

    public List<SystemTagInventory> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<SystemTagInventory> systemTags) {
        this.systemTags = systemTags;
    }

    public String getAttachedVmUuid() {
        return attachedVmUuid;
    }

    public void setAttachedVmUuid(String attachedVmUuid) {
        this.attachedVmUuid = attachedVmUuid;
    }

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
    }

    public void setVmSystemTags(List<String> vmSystemTags) {
        this.vmSystemTags = vmSystemTags;
    }

    public List<String> getVmSystemTags() {
        return vmSystemTags;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getRootImageUuid() {
        return rootImageUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }
}
