package org.zstack.billing.generator.pcidevice;

import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageAO;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageVO;
import org.zstack.billing.generator.UsageHistory;
import javax.persistence.*;

/**
 * Created by lining on 2019/3/28.
 */

@Entity
@Table
public class PciDeviceUsageHistoryVO extends PciDeviceUsageAO implements UsageHistory {
    public PciDeviceUsageHistoryVO() {
    }

    public PciDeviceUsageHistoryVO(PciDeviceUsageHistoryVO other) {
        super(other);
    }

    public PciDeviceUsageHistoryVO(PciDeviceUsageVO other) {
        this.setAccountUuid(other.getAccountUuid());
        this.setCreateDate(other.getCreateDate());
        this.setDateInLong(other.getDateInLong());
        this.setDescription(other.getDescription());
        this.setDeviceId(other.getDeviceId());
        this.setInventory(other.getInventory());
        this.setLastOpDate(other.getLastOpDate());
        this.setPciDeviceUuid(other.getPciDeviceUuid());
        this.setStatus(other.getStatus());
        this.setSubdeviceId(other.getSubdeviceId());
        this.setSubvendorId(other.getSubvendorId());
        this.setVendorId(other.getVendorId());
        this.setVmName(other.getVmName());
        this.setVmUuid(other.getVmUuid());
    }
}
