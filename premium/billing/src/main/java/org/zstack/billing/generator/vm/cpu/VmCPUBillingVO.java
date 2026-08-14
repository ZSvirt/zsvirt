package org.zstack.billing.generator.vm.cpu;

import org.zstack.billing.generator.BillingVO;
import javax.persistence.*;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class VmCPUBillingVO extends BillingVO {

    @Column
    private int cpuNum;

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }
}
