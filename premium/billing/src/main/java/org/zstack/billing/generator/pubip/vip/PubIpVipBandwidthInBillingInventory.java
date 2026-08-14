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
@Inventory(mappingVOClass = PubIpVipBandwidthInBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN)})
public class PubIpVipBandwidthInBillingInventory extends BillingInventory {
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

    public PubIpVipBandwidthInBillingInventory() {
    }

    public PubIpVipBandwidthInBillingInventory(PubIpVipBandwidthInBillingVO vo) {
        super(vo);
        this.setBandwidthSize(vo.getBandwidthSize());
        this.setVipIp(vo.getVipIp());
    }

    public static PubIpVipBandwidthInBillingInventory valueOf(PubIpVipBandwidthInBillingVO vo) {
        return new PubIpVipBandwidthInBillingInventory(vo);
    }

    public static List<PubIpVipBandwidthInBillingInventory> valueOf1(Collection<PubIpVipBandwidthInBillingVO> vos) {
        List<PubIpVipBandwidthInBillingInventory> invs = new ArrayList<PubIpVipBandwidthInBillingInventory>();
        for (PubIpVipBandwidthInBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
