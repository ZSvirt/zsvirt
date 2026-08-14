package org.zstack.ovf.datatype;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfVolumeInfo {
    private String name;
    private String diskId;
    private String driverType;

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }
}
