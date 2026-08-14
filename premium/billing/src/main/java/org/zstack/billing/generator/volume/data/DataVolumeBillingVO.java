package org.zstack.billing.generator.volume.data;

import org.zstack.billing.generator.BillingVO;

import javax.persistence.*;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class DataVolumeBillingVO extends BillingVO {
    @Column
    private long volumeSize;

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }
}
