package org.zstack.billing.spendingcalculator.vmnic;

import org.zstack.billing.UsageSample;

/**
 * Created by lining on 2018/11/20.
 */
public class VmNicBandwidthInUsageSample extends UsageSample {
    private String vmNicUuid;

    public String vmNicIp;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getVmNicIp() {
        return vmNicIp;
    }

    public void setVmNicIp(String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }
}
