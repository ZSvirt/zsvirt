package org.zstack.billing.spendingcalculator.vip;

import javax.persistence.*;

/**
 * Created by lining on 2018/11/16.
 */
@Entity
@Table
public class PubIpVipBandwidthUsageVO extends PubIpVipBandwidthUsageAO {
    public PubIpVipBandwidthUsageVO() {

    }

    public PubIpVipBandwidthUsageVO(PubIpVipBandwidthUsageVO other) {
        super(other);
    }
}
