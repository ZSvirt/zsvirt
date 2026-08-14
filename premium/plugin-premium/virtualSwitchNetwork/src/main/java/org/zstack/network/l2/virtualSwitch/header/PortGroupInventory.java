package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = PortGroupVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L3NetworkInventory.class, type = VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vSwitch", inventoryClass = L2VirtualSwitchNetworkInventory.class,
                foreignKey = "vSwitchUuid", expandedInventoryKey = "uuid"),
})
public class PortGroupInventory extends L3NetworkInventory {
    private String vSwitchUuid;
    private PortGroupVlanMode vlanMode;
    private Integer vlanId;
    private String vlanRanges;

    public PortGroupInventory() {
    }

    public PortGroupInventory(PortGroupVO vo) {
        super(vo);
        this.setvSwitchUuid(vo.getvSwitchUuid());
        this.setVlanMode(vo.getVlanMode());
        this.setVlanId(vo.getVlanId());
        this.setVlanRanges(vo.getVlanRanges());
    }

    public static PortGroupInventory valueOf(PortGroupVO vo) {
        return new PortGroupInventory(vo);
    }

    public static List<PortGroupInventory> valueOf1(Collection<PortGroupVO> vos) {
        List<PortGroupInventory> invs = new ArrayList<>(vos.size());
        for (PortGroupVO vo : vos) {
            invs.add(new PortGroupInventory(vo));
        }
        return invs;
    }

    public String getvSwitchUuid() {
        return vSwitchUuid;
    }

    public void setvSwitchUuid(String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public PortGroupVlanMode getVlanMode() {
        return vlanMode;
    }

    public void setVlanMode(PortGroupVlanMode vlanMode) {
        this.vlanMode = vlanMode;
    }

    public String getVlanRanges() {
        return vlanRanges;
    }

    public void setVlanRanges(String vlanRanges) {
        this.vlanRanges = vlanRanges;
    }
}
