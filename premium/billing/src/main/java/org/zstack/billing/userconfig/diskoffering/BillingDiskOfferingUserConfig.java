package org.zstack.billing.userconfig.diskoffering;

import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;

/**
 * Created by lining on 2019/4/16.
 */
public class BillingDiskOfferingUserConfig extends DiskOfferingUserConfig {
    private DiskOfferingPriceConfig priceUserConfig;

    public DiskOfferingPriceConfig getPriceUserConfig() {
        return priceUserConfig;
    }

    public void setPriceUserConfig(DiskOfferingPriceConfig priceUserConfig) {
        this.priceUserConfig = priceUserConfig;
    }
}

