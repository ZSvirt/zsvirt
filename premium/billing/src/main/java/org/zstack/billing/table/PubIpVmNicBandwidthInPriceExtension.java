package org.zstack.billing.table;

import org.zstack.billing.BillingConstants;

/**
 * Created by lining on 2019/11/2.
 */
public class PubIpVmNicBandwidthInPriceExtension extends BasePriceExtension {
    @Override
    public String getPriceResourceName() {
        return BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN;
    }
}
