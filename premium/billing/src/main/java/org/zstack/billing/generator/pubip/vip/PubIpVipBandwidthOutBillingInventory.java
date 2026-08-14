package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.generator.BillingInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/5/28.
 */
@Inventory(mappingVOClass = PubIpVipBandwidthOutBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT)})
public class PubIpVipBandwidthOutBillingInventory extends BillingInventory {
    private String vipIp;

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

    public PubIpVipBandwidthOutBillingInventory() {
    }

    public PubIpVipBandwidthOutBillingInventory(PubIpVipBandwidthOutBillingVO vo) {
        super(vo);
        this.setBandwidthSize(vo.getBandwidthSize());
        this.setVipIp(vo.getVipIp());
    }

    public static PubIpVipBandwidthOutBillingInventory valueOf(PubIpVipBandwidthOutBillingVO vo) {
        return new PubIpVipBandwidthOutBillingInventory(vo);
    }

    public static List<PubIpVipBandwidthOutBillingInventory> valueOf1(Collection<PubIpVipBandwidthOutBillingVO> vos) {
        List<PubIpVipBandwidthOutBillingInventory> invs = new ArrayList<PubIpVipBandwidthOutBillingInventory>();
        for (PubIpVipBandwidthOutBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
