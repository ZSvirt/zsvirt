package org.zstack.billing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.zstack.billing.userconfig.diskoffering.DiskOfferingPriceConfig;
import org.zstack.billing.userconfig.price.PriceUserConfig;
import org.zstack.header.configuration.userconfig.DiskOfferingAllocateConfig;
import org.zstack.header.configuration.userconfig.DiskOfferingDisplayAttributeConfig;


public class OfferingUserConfig {
    public DiskOfferingAllocatePoolNamesConfig allocate = new DiskOfferingAllocatePoolNamesConfig();

    public DiskOfferingPriceConfig priceUserConfig = new DiskOfferingPriceConfig();

    public DiskOfferingDisplayAttributeConfig displayAttribute = new DiskOfferingDisplayAttributeConfig();

    public OfferingUserConfig(DiskOfferingAllocatePoolNamesConfig allocate, DiskOfferingPriceConfig priceUserConfig, DiskOfferingDisplayAttributeConfig displayAttribute){
        this.allocate = allocate;
        this.priceUserConfig = priceUserConfig;
        this.displayAttribute = displayAttribute;
    }

    public void setPriceUserConfig(DiskOfferingPriceConfig priceUserConfig){
        this.priceUserConfig = priceUserConfig;
    }

    public DiskOfferingPriceConfig getPriceUserConfig(){
        return priceUserConfig;
    }


    public void setAllocate(DiskOfferingAllocatePoolNamesConfig allocate){
        this.allocate = allocate;
    }

    public DiskOfferingAllocatePoolNamesConfig getAllocate(){
        return allocate;
    }

    public void setDisplayAttribute(DiskOfferingDisplayAttributeConfig displayAttribute){
        this.displayAttribute = displayAttribute;
    }

    public DiskOfferingDisplayAttributeConfig getDisplayAttribute(){
        return displayAttribute;
    }
}
