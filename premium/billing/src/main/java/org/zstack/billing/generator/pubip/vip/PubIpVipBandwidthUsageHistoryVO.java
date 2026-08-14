package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.generator.UsageHistory;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageAO;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageVO;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by lining on 2019/4/1.
 */

@Entity
@Table
public class PubIpVipBandwidthUsageHistoryVO extends PubIpVipBandwidthUsageAO implements UsageHistory {

    public PubIpVipBandwidthUsageHistoryVO() {

    }

    public PubIpVipBandwidthUsageHistoryVO(PubIpVipBandwidthUsageHistoryVO other) {
        super(other);
    }

    public PubIpVipBandwidthUsageHistoryVO(PubIpVipBandwidthUsageVO other) {
        this.setAccountUuid(other.getAccountUuid());
        this.setDateInLong(other.getDateInLong());
        this.setBandwidthIn(other.getBandwidthIn());
        this.setBandwidthOut(other.getBandwidthOut());
        this.setCreateDate(other.getCreateDate());
        this.setInventory(other.getInventory());
        this.setLastOpDate(other.getLastOpDate());
        this.setL3NetworkUuid(other.getL3NetworkUuid());
        this.setVipIp(other.getVipIp());
        this.setVipName(other.getVipName());
        this.setVipStatus(other.getVipStatus());
        this.setVipUuid(other.getVipUuid());
    }
}
