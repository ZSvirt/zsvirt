package org.zstack.billing.generator.volume.data;

import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageAO;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageVO;
import org.zstack.billing.generator.UsageHistory;
import javax.persistence.*;

/**
 * Created by lining on 2019/3/28.
 */

@Entity
@Table
public class DataVolumeUsageHistoryVO extends DataVolumeUsageAO implements UsageHistory{
    @Column
    protected String resourcePriceUserConfig;

    public String getResourcePriceUserConfig() {
        return resourcePriceUserConfig;
    }

    public void setResourcePriceUserConfig(String resourcePriceUserConfig) {
        this.resourcePriceUserConfig = resourcePriceUserConfig;
    }

    public DataVolumeUsageHistoryVO() {

    }

    public DataVolumeUsageHistoryVO(DataVolumeUsageHistoryVO vo) {
        super(vo);
    }

    public DataVolumeUsageHistoryVO(DataVolumeUsageVO vo) {
        this.setAccountUuid(vo.getAccountUuid());
        this.setDateInLong(vo.getDateInLong());
        this.setVolumeUuid(vo.getVolumeUuid());
        this.setVolumeStatus(vo.getVolumeStatus());
        this.setVolumeName(vo.getVolumeName());
        this.setVolumeSize(vo.getVolumeSize());
        this.setInventory(vo.getInventory());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public DataVolumeUsageHistoryVO(DataVolumeUsageExtensionVO vo) {
        this.setAccountUuid(vo.getAccountUuid());
        this.setDateInLong(vo.getDateInLong());
        this.setVolumeUuid(vo.getVolumeUuid());
        this.setVolumeStatus(vo.getVolumeStatus());
        this.setVolumeName(vo.getVolumeName());
        this.setVolumeSize(vo.getVolumeSize());
        this.setInventory(vo.getInventory());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setResourcePriceUserConfig(vo.getResourcePriceUserConfig());
    }
}
