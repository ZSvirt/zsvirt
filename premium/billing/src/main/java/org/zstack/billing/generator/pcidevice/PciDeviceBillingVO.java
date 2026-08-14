package org.zstack.billing.generator.pcidevice;

import org.zstack.billing.generator.BillingVO;
import javax.persistence.*;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class PciDeviceBillingVO extends BillingVO {
    @Column
    private String vmName;

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }
}
