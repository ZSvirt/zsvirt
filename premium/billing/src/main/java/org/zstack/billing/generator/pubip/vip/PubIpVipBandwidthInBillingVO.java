package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.generator.BillingVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by lining on 2019/3/29.
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name="id", referencedColumnName = "id")
public class PubIpVipBandwidthInBillingVO extends BillingVO{
    @Column
    private String vipIp;

    @Column
    private long bandwidthSize;

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public long getBandwidthSize() {
        return bandwidthSize;
    }

    public void setBandwidthSize(long bandwidthSize) {
        this.bandwidthSize = bandwidthSize;
    }
}
