package org.zstack.network.service.vip;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 02/11/2017
 */
@Inventory(mappingVOClass = VipPeerL3NetworkRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
})
public class VipPeerL3NetworkRefInventory implements Serializable {
    private String vipUuid;

    private String l3NetworkUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public static VipPeerL3NetworkRefInventory valueOf(VipPeerL3NetworkRefVO vo) {
        VipPeerL3NetworkRefInventory inv = new VipPeerL3NetworkRefInventory();
        inv.setVipUuid(vo.getVipUuid());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public static List<VipPeerL3NetworkRefInventory> valueOf(Collection<VipPeerL3NetworkRefVO> vos) {
        List<VipPeerL3NetworkRefInventory> invs = new ArrayList<>(vos.size());
        for (VipPeerL3NetworkRefVO vo : vos) {
            invs.add(VipPeerL3NetworkRefInventory.valueOf(vo));
        }
        return invs;
    }
}
