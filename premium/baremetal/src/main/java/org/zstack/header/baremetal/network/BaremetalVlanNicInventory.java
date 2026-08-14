package org.zstack.header.baremetal.network;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = BaremetalVlanNicVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BaremetalNicInventory.class, type = L2NetworkConstant.L2_VLAN_NETWORK_TYPE)})
public class BaremetalVlanNicInventory extends BaremetalNicInventory {
    private Integer vlan;

    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }

    public BaremetalVlanNicInventory() {

    }

    public BaremetalVlanNicInventory(BaremetalVlanNicVO vo) {
        super(vo);
        this.setVlan(vo.getVlan());
    }

    public static BaremetalVlanNicInventory valueOf(BaremetalVlanNicVO vo) {
        return new BaremetalVlanNicInventory(vo);
    }

    public static List<BaremetalVlanNicInventory> valueOf1(Collection<BaremetalVlanNicVO> vos) {
        return vos.stream().map(BaremetalVlanNicInventory::valueOf).collect(Collectors.toList());
    }
}
