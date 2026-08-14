package org.zstack.billing.generator.pubip.vmnic;

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
@Inventory(mappingVOClass = PubIpVmNicBandwidthOutBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT)})
public class PubIpVmNicBandwidthOutBillingInventory extends BillingInventory {
    private String vmNicIp;

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

    public PubIpVmNicBandwidthOutBillingInventory() {
    }

    public PubIpVmNicBandwidthOutBillingInventory(PubIpVmNicBandwidthOutBillingVO vo) {
        super(vo);
        this.setBandwidthSize(vo.getBandwidthSize());
        this.setVmNicIp(vo.getVmNicIp());
    }

    public static PubIpVmNicBandwidthOutBillingInventory valueOf(PubIpVmNicBandwidthOutBillingVO vo) {
        return new PubIpVmNicBandwidthOutBillingInventory(vo);
    }

    public static List<PubIpVmNicBandwidthOutBillingInventory> valueOf1(Collection<PubIpVmNicBandwidthOutBillingVO> vos) {
        List<PubIpVmNicBandwidthOutBillingInventory> invs = new ArrayList<PubIpVmNicBandwidthOutBillingInventory>();
        for (PubIpVmNicBandwidthOutBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
