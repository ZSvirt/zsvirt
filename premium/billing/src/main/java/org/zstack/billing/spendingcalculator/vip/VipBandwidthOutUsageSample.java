package org.zstack.billing.spendingcalculator.vip;

import org.zstack.billing.UsageSample;

/**
 * Created by lining on 2018/11/20.
 */
public class VipBandwidthOutUsageSample extends UsageSample {
    private String vipUuid;

    private String vipIp;

    private String vipName;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getVipName() {
        return vipName;
    }

    public void setVipName(String vipName) {
        this.vipName = vipName;
    }
}
