package org.zstack.billing.userconfig.diskoffering;

import org.zstack.billing.userconfig.price.PriceUserConfig;

/**
 * Created by lining on 2019/4/17.
 */
public class DiskOfferingPriceConfig {
    private PriceUserConfig volume;

    public PriceUserConfig getVolume() {
        return volume;
    }

    public void setVolume(PriceUserConfig volume) {
        this.volume = volume;
    }
}
