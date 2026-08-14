package org.zstack.billing.generator.pubip.vmnic;

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
public class PubIpVmNicBandwidthOutBillingVO extends BillingVO{

    @Column
    private String vmNicIp;

    @Column
    private long bandwidthSize;

    public String getVmNicIp() {
        return vmNicIp;
    }

    public void setVmNicIp(String vmNicIp) {
        this.vmNicIp = vmNicIp;
    }

    public long getBandwidthSize() {
        return bandwidthSize;
    }

    public void setBandwidthSize(long bandwidthSize) {
        this.bandwidthSize = bandwidthSize;
    }

}
