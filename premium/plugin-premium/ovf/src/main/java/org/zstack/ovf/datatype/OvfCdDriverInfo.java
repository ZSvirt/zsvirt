package org.zstack.ovf.datatype;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfCdDriverInfo {
    private Boolean autoAllocation;
    private String driverType;
    private String subType;
    private String name;

    public Boolean getAutoAllocation() {
        return autoAllocation;
    }

    public void setAutoAllocation(Boolean autoAllocation) {
        this.autoAllocation = autoAllocation;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
