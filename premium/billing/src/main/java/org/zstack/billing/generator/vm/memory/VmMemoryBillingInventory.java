package org.zstack.billing.generator.vm.memory;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.generator.BillingInventory;
import org.zstack.billing.generator.vm.cpu.VmCPUBillingVO;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/5/28.
 */
@Inventory(mappingVOClass = VmMemoryBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_MEMORY)})
public class VmMemoryBillingInventory extends BillingInventory {
    private long memorySize;

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public VmMemoryBillingInventory() {
    }

    public VmMemoryBillingInventory(VmMemoryBillingVO vo) {
        super(vo);
        this.setMemorySize(vo.getMemorySize());
    }

    public static VmMemoryBillingInventory valueOf(VmMemoryBillingVO vo) {
        return new VmMemoryBillingInventory(vo);
    }

    public static List<VmMemoryBillingInventory> valueOf1(Collection<VmMemoryBillingVO> vos) {
        List<VmMemoryBillingInventory> invs = new ArrayList<VmMemoryBillingInventory>();
        for (VmMemoryBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
