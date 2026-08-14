package org.zstack.billing.spendingcalculator.volume.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by lining on 2019/5/10.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class DataVolumeUsageExtensionVO extends DataVolumeUsageVO {
    @Column
    protected String resourcePriceUserConfig;

    public String getResourcePriceUserConfig() {
        return resourcePriceUserConfig;
    }

    public void setResourcePriceUserConfig(String resourcePriceUserConfig) {
        this.resourcePriceUserConfig = resourcePriceUserConfig;
    }

    public DataVolumeUsageExtensionVO() {

    }

    public DataVolumeUsageExtensionVO(DataVolumeUsageVO other) {
        super(other);
    }
}
