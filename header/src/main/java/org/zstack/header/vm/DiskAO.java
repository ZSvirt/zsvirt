package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.SDK;
import org.zstack.header.volume.VolumeVO;

import java.util.List;

@SDK(sdkClassName = "DiskAO")
@PythonClassInventory
public class DiskAO {
    private boolean boot;
    private String platform;
    private String guestOsType;
    private String architecture;
    private String primaryStorageUuid;
    private long size;
    /**
     * allow: ImageVO.uuid
     */
    private String templateUuid;
    private String diskOfferingUuid;
    private String sourceType;
    /**
     * allow: VolumeVO.uuid
     */
    private String sourceUuid;
    private List<String> systemTags;
    private String name;

    public DiskAO withImage(String imageUuid) {
        this.templateUuid = imageUuid;
        return this;
    }

    public DiskAO withVolume(String volumeUuid) {
        this.sourceType = VolumeVO.class.getSimpleName();
        this.sourceUuid = volumeUuid;
        return this;
    }

    public DiskAO withLun(String lunUuid) {
        this.sourceType = "LunVO";
        this.sourceUuid = lunUuid;
        return this;
    }

    public boolean isBoot() {
        return boot;
    }

    public void setBoot(boolean boot) {
        this.boot = boot;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static DiskAO rootDisk() {
        DiskAO disk = new DiskAO();
        disk.setBoot(true);
        return disk;
    }

    public static DiskAO nonRootDisk() {
        return new DiskAO();
    }
}
