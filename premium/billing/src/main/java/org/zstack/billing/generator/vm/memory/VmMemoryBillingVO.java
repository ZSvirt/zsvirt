package org.zstack.billing.generator.vm.memory;

import org.zstack.billing.generator.BillingVO;
import javax.persistence.*;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class VmMemoryBillingVO extends BillingVO {
    @Column
    private long memorySize;

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }
}
