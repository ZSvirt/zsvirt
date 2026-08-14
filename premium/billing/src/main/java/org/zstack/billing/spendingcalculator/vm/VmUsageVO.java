package org.zstack.billing.spendingcalculator.vm;

import javax.persistence.*;

/**
 * Created by frank on 3/1/2016.
 */
@Table
@Entity
public class VmUsageVO extends VmUsageAO {
    public VmUsageVO() {

    }

    public VmUsageVO(VmUsageVO other) {
        super(other);
    }
}
