package org.zstack.header.network.l2;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory
 * @category
 * @example {
 * "org.zstack.header.network.l2.APICreateL2VlanNetworkEvent": {
 * "inventory": {
 * "vlan": 10,
 * "uuid": "14a01b0978684b2ea6e5a355c7c7fd73",
 * "name": "TestL2VlanNetwork",
 * "description": "Test",
 * "zoneUuid": "c74f8ff8a4c5456b852713b82c034074",
 * "physicalInterface": "eth0",
 * "type": "L2VlanNetwork",
 * "createDate": "May 4, 2014 4:31:47 PM",
 * "lastOpDate": "May 4, 2014 4:31:47 PM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@PythonClassInventory
@Inventory(mappingVOClass = L2VlanNetworkVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = L2NetworkConstant.L2_VLAN_NETWORK_TYPE)})
public class L2VlanNetworkInventory extends L2NetworkInventory {
    /**
     * @desc vlan id
     * @choices [0, 4095]
     */
    private Integer vlan;

    public L2VlanNetworkInventory() {
    }

    protected L2VlanNetworkInventory(L2VlanNetworkVO vo) {
        super(vo);
        this.setVlan(vo.getVlan());
    }

    public static L2VlanNetworkInventory valueOf(L2VlanNetworkVO vo) {
        return new L2VlanNetworkInventory(vo);
    }

    public static List<L2VlanNetworkInventory> valueOf1(Collection<L2VlanNetworkVO> vos) {
        List<L2VlanNetworkInventory> invs = new ArrayList<L2VlanNetworkInventory>(vos.size());
        for (L2VlanNetworkVO vo : vos) {
            invs.add(new L2VlanNetworkInventory(vo));
        }
        return invs;
    }

    public int getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }
}
