package org.zstack.billing.generator.vm;

import org.zstack.billing.spendingcalculator.vm.VmUsageAO;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO;
import org.zstack.billing.generator.UsageHistory;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by lining on 2019/4/1.
 */

@Entity
@Table
public class VmUsageHistoryVO extends VmUsageAO implements UsageHistory {

    public VmUsageHistoryVO() {

    }

    public VmUsageHistoryVO(VmUsageHistoryVO other) {
        super(other);
    }

    public VmUsageHistoryVO(VmUsageVO vmUsageVO) {
        this.setAccountUuid(vmUsageVO.getAccountUuid());
        this.setDateInLong(vmUsageVO.getDateInLong());
        this.setLastOpDate(vmUsageVO.getLastOpDate());
        this.setInventory(vmUsageVO.getInventory());
        this.setCreateDate(vmUsageVO.getCreateDate());
        this.setCpuNum(vmUsageVO.getCpuNum());
        this.setMemorySize(vmUsageVO.getMemorySize());
        this.setName(vmUsageVO.getName());
        this.setRootVolumeSize(vmUsageVO.getRootVolumeSize());
        this.setVmUuid(vmUsageVO.getVmUuid());
        this.setState(vmUsageVO.getState());
    }
}
