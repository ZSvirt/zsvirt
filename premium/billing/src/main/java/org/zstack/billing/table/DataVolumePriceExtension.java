package org.zstack.billing.table;

import org.zstack.billing.BillingConstants;

/**
 * Created by lining on 2019/11/2.
 */
public class DataVolumePriceExtension extends VolumePriceExtension {
    @Override
    public String getPriceResourceName() {
        return BillingConstants.SPENDING_TYPE_DATA_VOLUME;
    }
}
