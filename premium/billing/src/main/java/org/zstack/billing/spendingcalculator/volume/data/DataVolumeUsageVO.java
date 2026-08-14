package org.zstack.billing.spendingcalculator.volume.data;

import javax.persistence.*;

/**
 * Created by frank on 3/1/2016.
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class DataVolumeUsageVO extends DataVolumeUsageAO {
    public DataVolumeUsageVO() {

    }

    public DataVolumeUsageVO(DataVolumeUsageVO other) {
        super(other);
    }
}
