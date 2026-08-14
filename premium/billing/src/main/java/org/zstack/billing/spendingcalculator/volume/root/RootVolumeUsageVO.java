package org.zstack.billing.spendingcalculator.volume.root;

import javax.persistence.*;

/**
 * Created by frank on 3/1/2016.
 */
@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class RootVolumeUsageVO extends RootVolumeUsageAO {
    public RootVolumeUsageVO() {

    }

    public RootVolumeUsageVO(RootVolumeUsageVO other) {
        super(other);
    }
}
