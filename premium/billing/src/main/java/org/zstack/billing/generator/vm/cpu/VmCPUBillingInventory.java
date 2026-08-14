package org.zstack.billing.generator.vm.cpu;

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
@Inventory(mappingVOClass = VmCPUBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_CPU)})
public class VmCPUBillingInventory extends BillingInventory {
    private int cpuNum;

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public VmCPUBillingInventory() {
    }

    public VmCPUBillingInventory(VmCPUBillingVO vo) {
        super(vo);
        this.setCpuNum(vo.getCpuNum());
    }

    public static VmCPUBillingInventory valueOf(VmCPUBillingVO vo) {
        return new VmCPUBillingInventory(vo);
    }

    public static List<VmCPUBillingInventory> valueOf1(Collection<VmCPUBillingVO> vos) {
        List<VmCPUBillingInventory> invs = new ArrayList<VmCPUBillingInventory>();
        for (VmCPUBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
