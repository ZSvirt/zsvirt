package org.zstack.billing.spendingcalculator.vmnic;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by lining on 2018/11/20.
 */
public class PubIpVmNicBandwidthSpending extends SpendingDetails {
    public String vmNicIp;
    public List<VmNicBandwidthSpendingDetails> bandwidthInInventory;
    public List<VmNicBandwidthSpendingDetails> bandwidthOutInventory;

    public List<VmNicBandwidthSpendingDetails> getBandwidthInInventory() {
        return bandwidthInInventory;
    }

    public void setBandwidthInInventory(List<VmNicBandwidthSpendingDetails> bandwidthInInventory) {
        this.bandwidthInInventory = bandwidthInInventory;
    }

    public List<VmNicBandwidthSpendingDetails> getBandwidthOutInventory() {
        return bandwidthOutInventory;
    }

    public void setBandwidthOutInventory(List<VmNicBandwidthSpendingDetails> bandwidthOutInventory) {
        this.bandwidthOutInventory = bandwidthOutInventory;
    }

    public String getVmNicIp() {
        return vmNicIp;
    }

    public void setVmNicIp(String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }
}
