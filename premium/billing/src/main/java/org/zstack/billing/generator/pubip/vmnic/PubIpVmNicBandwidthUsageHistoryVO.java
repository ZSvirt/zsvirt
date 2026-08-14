package org.zstack.billing.generator.pubip.vmnic;

import org.zstack.billing.generator.UsageHistory;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageAO;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageVO;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by lining on 2019/4/1.
 */

@Entity
@Table
public class PubIpVmNicBandwidthUsageHistoryVO extends PubIpVmNicBandwidthUsageAO implements UsageHistory {
    public PubIpVmNicBandwidthUsageHistoryVO() {

    }

    public PubIpVmNicBandwidthUsageHistoryVO(PubIpVmNicBandwidthUsageHistoryVO other) {
        super(other);
    }

    public PubIpVmNicBandwidthUsageHistoryVO(PubIpVmNicBandwidthUsageVO other) {
        this.setAccountUuid(other.getAccountUuid());
        this.setDateInLong(other.getDateInLong());
        this.setBandwidthIn(other.getBandwidthIn());
        this.setBandwidthOut(other.getBandwidthOut());
        this.setCreateDate(other.getCreateDate());
        this.setInventory(other.getInventory());
        this.setLastOpDate(other.getLastOpDate());
        this.setL3NetworkUuid(other.getL3NetworkUuid());
        this.setVmInstanceUuid(other.getVmInstanceUuid());
        this.setVmNicIp(other.getVmNicIp());
        this.setVmNicUuid(other.getVmNicUuid());
        this.setVmNicStatus(other.getVmNicStatus());
    }

}
