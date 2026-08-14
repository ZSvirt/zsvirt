package org.zstack.header.configuration.userconfig;

/**
 * Created by lining on 2019/4/16.
 */
public class DiskOfferingUserConfig {
    private DiskOfferingAllocateConfig allocate;

    private DiskOfferingDisplayAttributeConfig displayAttribute;

    public DiskOfferingAllocateConfig getAllocate() {
        return allocate;
    }

    public void setAllocate(DiskOfferingAllocateConfig allocate) {
        this.allocate = allocate;
    }

    public DiskOfferingDisplayAttributeConfig getDisplayAttribute() {
        return displayAttribute;
    }

    public void setDisplayAttribute(DiskOfferingDisplayAttributeConfig displayAttribute) {
        this.displayAttribute = displayAttribute;
    }
}

