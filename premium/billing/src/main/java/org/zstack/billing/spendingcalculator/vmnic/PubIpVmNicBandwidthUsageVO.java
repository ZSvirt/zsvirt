package org.zstack.billing.spendingcalculator.vmnic;

import javax.persistence.*;

/**
 * Created by lining on 2018/11/16.
 */
@Entity
@Table
public class PubIpVmNicBandwidthUsageVO extends PubIpVmNicBandwidthUsageAO {

    public PubIpVmNicBandwidthUsageVO() {

    }

    public PubIpVmNicBandwidthUsageVO(PubIpVmNicBandwidthUsageVO other) {
        super(other);
    }
}
