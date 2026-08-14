package org.zstack.billing.generator.volume.root;

import org.zstack.billing.generator.BillingVO;

import javax.persistence.*;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class RootVolumeBillingVO extends BillingVO {
    @Column
    private String vmInstanceUuid;

    @Column
    private long volumeSize;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }
}
