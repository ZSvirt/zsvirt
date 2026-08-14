package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = L2PortGroupNetworkVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(target = L3NetworkInventory.class, expandedField = "portGroup",
                inventoryClass = L2PortGroupNetworkInventory.class, foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid")
})
public class L2PortGroupNetworkInventory extends L2NetworkInventory {
    private String vSwitchUuid;
    private PortGroupVlanMode vlanMode;
    private Integer vlanId;
    private String vlanRanges;

    public L2PortGroupNetworkInventory() {
    }

    public L2PortGroupNetworkInventory(L2PortGroupNetworkVO vo) {
        super(vo);
        this.setvSwitchUuid(vo.getvSwitchUuid());
        this.setVlanMode(vo.getVlanMode());
        this.setVlanId(vo.getVlanId());
        this.setVlanRanges(vo.getVlanRanges());
    }

    public static L2PortGroupNetworkInventory valueOf(L2PortGroupNetworkVO vo) {
        return new L2PortGroupNetworkInventory(vo);
    }

    public static List<L2PortGroupNetworkInventory> valueOf1(Collection<L2PortGroupNetworkVO> vos) {
        List<L2PortGroupNetworkInventory> invs = new ArrayList<>(vos.size());
        for (L2PortGroupNetworkVO vo : vos) {
            invs.add(new L2PortGroupNetworkInventory(vo));
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
