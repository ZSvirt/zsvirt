package org.zstack.network.l2.vxlan.vxlanNetwork;


import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VxlanNetworkVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = VxlanNetworkConstant.VXLAN_NETWORK_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = L2VxlanNetworkPoolInventory.class,
                foreignKey = "poolUuid", expandedInventoryKey = "uuid")
})
public class L2VxlanNetworkInventory extends L2NetworkInventory {
    /**
     * @desc vlan id
     * @choices [1, 16777215]
     */
    private Integer vni;

    private String poolUuid;

    public L2VxlanNetworkInventory() {
    }

    protected L2VxlanNetworkInventory(VxlanNetworkVO vo) {
        super(vo);
        this.setVni(vo.getVni());
        this.setPoolUuid(vo.getPoolUuid());
    }

    public static L2VxlanNetworkInventory valueOf(VxlanNetworkVO vo) {
        return new L2VxlanNetworkInventory(vo);
    }

    public static List<L2VxlanNetworkInventory> valueOf1(Collection<VxlanNetworkVO> vos) {
        List<L2VxlanNetworkInventory> invs = new ArrayList<L2VxlanNetworkInventory>(vos.size());
        for (VxlanNetworkVO vo : vos) {
            invs.add(new L2VxlanNetworkInventory(vo));
        }
        return invs;
    }

    public int getVni() {
        return vni;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

}
