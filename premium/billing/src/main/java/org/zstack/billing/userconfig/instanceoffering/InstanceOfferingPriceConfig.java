package org.zstack.billing.userconfig.instanceoffering;


import org.zstack.billing.userconfig.price.PriceUserConfig;

/**
 * Created by lining on 2019/4/17.
 */
public class InstanceOfferingPriceConfig {
    private PriceUserConfig rootVolume;

    public PriceUserConfig getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(PriceUserConfig rootVolume) {
        this.rootVolume = rootVolume;
    }
}
