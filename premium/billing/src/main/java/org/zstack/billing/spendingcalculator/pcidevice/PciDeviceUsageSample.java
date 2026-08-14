package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.UsageSample;

/**
 * Created by shixin.ruan on 2018/05/05.
 */
public class PciDeviceUsageSample extends UsageSample {
    protected String pciDeviceUuid;
    protected String description;
    protected String vmName;

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
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
}
