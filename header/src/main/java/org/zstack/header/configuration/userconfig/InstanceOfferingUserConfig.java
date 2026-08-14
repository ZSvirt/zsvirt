package org.zstack.header.configuration.userconfig;

/**
 * Created by lining on 2019/4/16.
 */
public class InstanceOfferingUserConfig {
    InstanceOfferingAllocateConfig allocate;

    private InstanceOfferingDisplayAttributeConfig displayAttribute;

    public InstanceOfferingAllocateConfig getAllocate() {
        return allocate;
    }

    public void setAllocate(InstanceOfferingAllocateConfig allocate) {
        this.allocate = allocate;
    }

    public InstanceOfferingDisplayAttributeConfig getDisplayAttribute() {
        return displayAttribute;
    }

    public void setDisplayAttribute(InstanceOfferingDisplayAttributeConfig displayAttribute) {
        this.displayAttribute = displayAttribute;
    }
}