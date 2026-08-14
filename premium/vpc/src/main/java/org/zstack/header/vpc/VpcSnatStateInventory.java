package org.zstack.header.vpc;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcSnatStateVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vpc", inventoryClass = VpcRouterVmInventory.class,
                foreignKey = "vpcRouterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid")
})
public class VpcSnatStateInventory {
    private String uuid;
    private String vpcUuid;
    private String l3NetworkUuid;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcSnatStateInventory valueOf(VpcSnatStateVO vo) {
        VpcSnatStateInventory inv = new VpcSnatStateInventory();
        inv.setUuid(vo.getUuid());
        inv.setVpcUuid(vo.getVpcUuid());
        inv.setL3NetworkUuid((vo.getL3NetworkUuid()));
        inv.setState(vo.getState());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<VpcSnatStateInventory> valueOf(Collection<VpcSnatStateVO> vos) {
        List<VpcSnatStateInventory> invs = new ArrayList<>();
        for (VpcSnatStateVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVpcUuid() {
        return vpcUuid;
    }

    public void setVpcUuid(String vpcUuid) {
        this.vpcUuid = vpcUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
}
