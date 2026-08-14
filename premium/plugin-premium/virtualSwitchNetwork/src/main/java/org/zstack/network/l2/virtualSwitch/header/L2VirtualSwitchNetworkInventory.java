package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = L2VirtualSwitchNetworkVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE)})
public class L2VirtualSwitchNetworkInventory extends L2NetworkInventory {

    /**
     * @desc vlan id
     * @choices [0, 4095]
     */
    private Boolean isDistributed;

    private Integer vSwitchIndex;

    @Queryable(mappingClass = PortGroupInventory.class,
            joinColumn = @JoinColumn(name = "vSwitchUuid"))
    private List<PortGroupInventory> portGroups;

    public L2VirtualSwitchNetworkInventory() {
    }

    public L2VirtualSwitchNetworkInventory(L2VirtualSwitchNetworkVO vo) {
        super(vo);
        this.isDistributed = vo.getDistributed();
        this.vSwitchIndex = vo.getVSwitchIndex();
        this.setPortGroups(PortGroupInventory.valueOf1(vo.getPortGroups()));
    }

    public static L2VirtualSwitchNetworkInventory valueOf(L2VirtualSwitchNetworkVO vo) {
        return new L2VirtualSwitchNetworkInventory(vo);
    }

    public static List<L2VirtualSwitchNetworkInventory> valueOf1(Collection<L2VirtualSwitchNetworkVO> vos) {
        List<L2VirtualSwitchNetworkInventory> invs = new ArrayList<>(vos.size());
        for (L2VirtualSwitchNetworkVO vo : vos) {
            invs.add(new L2VirtualSwitchNetworkInventory(vo));
        }
        return invs;
    }

    public Boolean getDistributed() {
        return isDistributed;
    }

    public void setDistributed(Boolean distributed) {
        isDistributed = distributed;
    }

    public Integer getVSwitchIndex() {
        return vSwitchIndex;
    }

    public void setVSwitchIndex(Integer index) {
        this.vSwitchIndex = index;
    }

    public List<PortGroupInventory> getPortGroups() {
        return portGroups;
    }

    public void setPortGroups(List<PortGroupInventory> portGroups) {
        this.portGroups = portGroups;
    }
}
