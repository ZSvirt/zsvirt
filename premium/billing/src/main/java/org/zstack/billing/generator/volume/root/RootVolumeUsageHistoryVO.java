package org.zstack.billing.generator.volume.root;

import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageAO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageVO;
import org.zstack.billing.generator.UsageHistory;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by lining on 2019/4/1.
 */

@Entity
@Table
public class RootVolumeUsageHistoryVO extends RootVolumeUsageAO implements UsageHistory {

    @Column
    protected String resourcePriceUserConfig;

    public String getResourcePriceUserConfig() {
        return resourcePriceUserConfig;
    }

    public void setResourcePriceUserConfig(String resourcePriceUserConfig) {
        this.resourcePriceUserConfig = resourcePriceUserConfig;
    }

    public RootVolumeUsageHistoryVO() {

    }

    public RootVolumeUsageHistoryVO(RootVolumeUsageHistoryVO other) {
        super(other);
    }

    public RootVolumeUsageHistoryVO(RootVolumeUsageVO vo) {
        this.setAccountUuid(vo.getAccountUuid());
        this.setDateInLong(vo.getDateInLong());
        this.setVolumeUuid(vo.getVolumeUuid());
        this.setVolumeStatus(vo.getVolumeStatus());
        this.setVolumeName(vo.getVolumeName());
        this.setVolumeSize(vo.getVolumeSize());
        this.setInventory(vo.getInventory());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setVmUuid(vo.getVmUuid());
    }

    public RootVolumeUsageHistoryVO(RootVolumeUsageExtensionVO vo) {
        this.setAccountUuid(vo.getAccountUuid());
        this.setDateInLong(vo.getDateInLong());
        this.setVolumeUuid(vo.getVolumeUuid());
        this.setVolumeStatus(vo.getVolumeStatus());
        this.setVolumeName(vo.getVolumeName());
        this.setVolumeSize(vo.getVolumeSize());
        this.setInventory(vo.getInventory());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setVmUuid(vo.getVmUuid());
        this.setResourcePriceUserConfig(vo.getResourcePriceUserConfig());
    }
}
