package org.zstack.network.service.eip;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = EipVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VmNicInventory.class, expandedField = "eip", inventoryClass = EipInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmNicUuid"),
        @ExpandedQuery(target = VipInventory.class, expandedField = "eip", inventoryClass = EipInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vipUuid")
})
public class EipInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String vmNicUuid;
    private String vipUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String state;
    private String vipIp;
    private String guestIp;

    public static EipInventory valueOf(EipVO vo) {
        EipInventory inv = new EipInventory();
        inv.setName(vo.getName());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setVipUuid(vo.getVipUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setDescription(vo.getDescription());
        inv.setUuid(vo.getUuid());
        inv.setState(vo.getState().toString());
        inv.setVipIp(vo.getVipIp());
        inv.setGuestIp(vo.getGuestIp());
        return inv;
    }

    public static List<EipInventory> valueOf(Collection<EipVO> vos) {
        List<EipInventory> invs = new ArrayList<EipInventory>();
        for (EipVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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
