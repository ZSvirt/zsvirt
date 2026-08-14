package org.zstack.billing.userconfig.instanceoffering;


import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig;

/**
 * Created by lining on 2019/4/16.
 */
public class BillingInstanceOfferingUserConfig extends InstanceOfferingUserConfig {

    private InstanceOfferingPriceConfig priceUserConfig;

    public InstanceOfferingPriceConfig getPriceUserConfig() {
        return priceUserConfig;
    }

    public void setPriceUserConfig(InstanceOfferingPriceConfig priceUserConfig) {
        this.priceUserConfig = priceUserConfig;
    }
}

