package org.zstack.network.securitygroup;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SecurityGroupL3NetworkRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "securityGroup", inventoryClass = SecurityGroupInventory.class,
                foreignKey = "securityGroupUuid", expandedInventoryKey = "uuid")
})
public class SecurityGroupL3NetworkRefInventory {
    private String uuid;
    private String l3NetworkUuid;
    private String securityGroupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SecurityGroupL3NetworkRefInventory valueOf(SecurityGroupL3NetworkRefVO vo) {
        SecurityGroupL3NetworkRefInventory inv = new SecurityGroupL3NetworkRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setSecurityGroupUuid(vo.getSecurityGroupUuid());
        return inv;
    }

    public static List<SecurityGroupL3NetworkRefInventory> valueOf(Collection<SecurityGroupL3NetworkRefVO> vos) {
        List<SecurityGroupL3NetworkRefInventory> invs = new ArrayList<SecurityGroupL3NetworkRefInventory>();
        for (SecurityGroupL3NetworkRefVO vo : vos) {
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
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
