package org.zstack.header.vpc.ha;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcHaGroupVipRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vpcHa", inventoryClass = VpcHaGroupInventory.class,
                foreignKey = "vpcHaRouterUuid", expandedInventoryKey = "uuid")
})

public class VpcHaGroupVipRefInventory {
    private Long id;
    private String vpcHaRouterUuid;
    private String vipUuid;
    private String l3NetworkUuid;
    private String ip;
    private String netmask;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcHaGroupVipRefInventory valueOf(VpcHaGroupVipRefVO vo) {
        VpcHaGroupVipRefInventory inv = new VpcHaGroupVipRefInventory();
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setVipUuid(vo.getVipUuid());
        inv.setIp(vo.getIp());
        inv.setNetmask(vo.getNetmask());
        inv.setVpcHaRouterUuid(vo.getVpcHaRouterUuid());
        return inv;
    }

    public static List<VpcHaGroupVipRefInventory> valueOf(Collection<VpcHaGroupVipRefVO> vos) {
        List<VpcHaGroupVipRefInventory> invs = new ArrayList<VpcHaGroupVipRefInventory>();
        for (VpcHaGroupVipRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getVpcHaRouterUuid() {
        return vpcHaRouterUuid;
    }

    public void setVpcHaRouterUuid(String vpcHaRouterUuid) {
        this.vpcHaRouterUuid = vpcHaRouterUuid;
    }

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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
