package org.zstack.header.vpc.ha;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcHaGroupMonitorIpVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vpcHa", inventoryClass = VpcHaGroupInventory.class,
                foreignKey = "vpcHaRouterUuid", expandedInventoryKey = "uuid")
})

public class VpcHaGroupMonitorIpInventory {
    private Long id;
    private String vpcHaRouterUuid;
    private String monitorIp;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcHaGroupMonitorIpInventory valueOf(VpcHaGroupMonitorIpVO vo) {
        VpcHaGroupMonitorIpInventory inv = new VpcHaGroupMonitorIpInventory();
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setMonitorIp(vo.getMonitorIp());
        inv.setVpcHaRouterUuid(vo.getVpcHaRouterUuid());
        return inv;
    }

    public static List<VpcHaGroupMonitorIpInventory> valueOf(Collection<VpcHaGroupMonitorIpVO> vos) {
        List<VpcHaGroupMonitorIpInventory> invs = new ArrayList<VpcHaGroupMonitorIpInventory>();
        for (VpcHaGroupMonitorIpVO vo : vos) {
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

    public String getMonitorIp() {
        return monitorIp;
    }

    public void setMonitorIp(String monitorIp) {
        this.monitorIp = monitorIp;
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
