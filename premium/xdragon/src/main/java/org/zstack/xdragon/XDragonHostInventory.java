package org.zstack.xdragon;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.kvm.KVMHostInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = XDragonHostVO.class, collectionValueOfMethod = "valueOf2",
        parent = {@Parent(inventoryClass = KVMHostInventory.class, type = XDragonConstant.HYPERVISOR_TYPE)})
public class XDragonHostInventory extends KVMHostInventory {
    private Long totalPhysicalMemory;

    protected XDragonHostInventory(XDragonHostVO vo) {
        super(vo);
        this.setTotalPhysicalMemory(vo.getTotalPhysicalMemory());
    }

    public XDragonHostInventory() {
    }

    public static XDragonHostInventory valueOf(XDragonHostVO vo) {
        return new XDragonHostInventory(vo);
    }

    public static List<XDragonHostInventory> valueOf2(Collection<XDragonHostVO> vos) {
        List<XDragonHostInventory> invs = new ArrayList<XDragonHostInventory>();
        for (XDragonHostVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getTotalPhysicalMemory() {
        return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(Long totalPhysicalMemory) {
        this.totalPhysicalMemory = totalPhysicalMemory;
    }
}
