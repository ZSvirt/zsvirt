package org.zstack.header.vpc.ha;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcHaGroupNetworkServiceRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vpcHa", inventoryClass = VpcHaGroupInventory.class,
                foreignKey = "vpcHaRouterUuid", expandedInventoryKey = "uuid")
})

public class VpcHaGroupNetworkServiceRefInventory {
    private Long id;
    private String vpcHaRouterUuid;
    private String networkServiceName;
    private String networkServiceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VpcHaGroupNetworkServiceRefInventory valueOf(VpcHaGroupNetworkServiceRefVO vo) {
        VpcHaGroupNetworkServiceRefInventory inv = new VpcHaGroupNetworkServiceRefInventory();
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setVpcHaRouterUuid(vo.getVpcHaRouterUuid());
        inv.setNetworkServiceName(vo.getNetworkServiceName());
        inv.setNetworkServiceUuid(vo.getNetworkServiceUuid());
        return inv;
    }

    public static List<VpcHaGroupNetworkServiceRefInventory> valueOf(Collection<VpcHaGroupNetworkServiceRefVO> vos) {
        List<VpcHaGroupNetworkServiceRefInventory> invs = new ArrayList<VpcHaGroupNetworkServiceRefInventory>();
        for (VpcHaGroupNetworkServiceRefVO vo : vos) {
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

    public String getNetworkServiceName() {
        return networkServiceName;
    }

    public void setNetworkServiceName(String networkServiceName) {
        this.networkServiceName = networkServiceName;
    }

    public String getNetworkServiceUuid() {
        return networkServiceUuid;
    }

    public void setNetworkServiceUuid(String networkServiceUuid) {
        this.networkServiceUuid = networkServiceUuid;
    }
}
