package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.UsageSample;

/**
 * Created by xing5 on 2016/6/8.
 */
public class VmUsageSample extends UsageSample {
    protected String vmUuid;
    protected String vmName;
    protected String hypervisorType;

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}
