package org.zstack.billing.generator.pcidevice;

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
@Inventory(mappingVOClass = PciDeviceBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_TYPE_PCI_DEVICE)})
public class PciDeviceBillingInventory extends BillingInventory {
    private String vmName;

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public PciDeviceBillingInventory() {
    }

    public PciDeviceBillingInventory(PciDeviceBillingVO vo) {
        super(vo);
        this.setVmName(vo.getVmName());
    }

    public static PciDeviceBillingInventory valueOf(PciDeviceBillingVO vo) {
        return new PciDeviceBillingInventory(vo);
    }

    public static List<PciDeviceBillingInventory> valueOf1(Collection<PciDeviceBillingVO> vos) {
        List<PciDeviceBillingInventory> invs = new ArrayList<PciDeviceBillingInventory>();
        for (PciDeviceBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
