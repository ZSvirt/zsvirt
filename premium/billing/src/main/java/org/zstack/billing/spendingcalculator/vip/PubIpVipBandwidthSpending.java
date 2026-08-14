package org.zstack.billing.spendingcalculator.vip;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by lining on 2018/11/20.
 */
public class PubIpVipBandwidthSpending extends SpendingDetails {
    public String vipIp;
    public List<VipBandwidthSpendingDetails> bandwidthInInventory;
    public List<VipBandwidthSpendingDetails> bandwidthOutInventory;

    public List<VipBandwidthSpendingDetails> getBandwidthInInventory() {
        return bandwidthInInventory;
    }

    public void setBandwidthInInventory(List<VipBandwidthSpendingDetails> bandwidthInInventory) {
        this.bandwidthInInventory = bandwidthInInventory;
    }

    public List<VipBandwidthSpendingDetails> getBandwidthOutInventory() {
        return bandwidthOutInventory;
    }

    public void setBandwidthOutInventory(List<VipBandwidthSpendingDetails> bandwidthOutInventory) {
        this.bandwidthOutInventory = bandwidthOutInventory;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }
}
