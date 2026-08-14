package org.zstack.billing.generator.pubip.vmnic;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.generator.BillingInventory;
import org.zstack.billing.generator.volume.data.DataVolumeBillingVO;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/5/28.
 */
@Inventory(mappingVOClass = PubIpVmNicBandwidthInBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN)})
public class PubIpVmNicBandwidthInBillingInventory extends BillingInventory {
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

    public PubIpVmNicBandwidthInBillingInventory() {
    }

    public PubIpVmNicBandwidthInBillingInventory(PubIpVmNicBandwidthInBillingVO vo) {
        super(vo);
        this.setBandwidthSize(vo.getBandwidthSize());
        this.setVmNicIp(vo.getVmNicIp());
    }

    public static PubIpVmNicBandwidthInBillingInventory valueOf(PubIpVmNicBandwidthInBillingVO vo) {
        return new PubIpVmNicBandwidthInBillingInventory(vo);
    }

    public static List<PubIpVmNicBandwidthInBillingInventory> valueOf1(Collection<PubIpVmNicBandwidthInBillingVO> vos) {
        List<PubIpVmNicBandwidthInBillingInventory> invs = new ArrayList<PubIpVmNicBandwidthInBillingInventory>();
        for (PubIpVmNicBandwidthInBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
