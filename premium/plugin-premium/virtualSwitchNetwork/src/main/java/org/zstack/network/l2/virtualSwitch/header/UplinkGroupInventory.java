package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkHostRefInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = UplinkGroupVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "bonding", inventoryClass = HostNetworkBondingInventory.class,
                foreignKey = "bondingUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "physicalInterface", inventoryClass = HostNetworkInterfaceInventory.class,
                foreignKey = "interfaceUuid", expandedInventoryKey = "uuid"),
})
public class UplinkGroupInventory extends L2NetworkHostRefInventory {
    private String interfaceName;
    private UplinkGroupType type;
    private String bondingUuid;
    private String interfaceUuid;

    public UplinkGroupInventory() {
    }

    public UplinkGroupInventory(UplinkGroupVO vo) {
        super(vo);
        this.setInterfaceName(vo.getInterfaceName());
        this.setType(vo.getType());
        this.setBondingUuid(vo.getBondingUuid());
        this.setInterfaceUuid(vo.getInterfaceUuid());
    }

    public static UplinkGroupInventory valueOf(UplinkGroupVO vo) {
        return new UplinkGroupInventory(vo);
    }

    public static List<UplinkGroupInventory> valueOf1(Collection<UplinkGroupVO> vos) {
        List<UplinkGroupInventory> invs = new ArrayList<>(vos.size());
        for (UplinkGroupVO vo : vos) {
            invs.add(new UplinkGroupInventory(vo));
        }
        return invs;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String name) {
        this.interfaceName = name;
    }

    public UplinkGroupType getType() {
        return type;
    }

    public void setType(UplinkGroupType type) {
        this.type = type;
    }

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
}
