package org.zstack.network.service.virtualrouter.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/22/2015.
 */
@Inventory(mappingVOClass = VirtualRouterLoadBalancerRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "virtualRouterVm", inventoryClass = VirtualRouterVmInventory.class,
                foreignKey = "virtualRouterVmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "loadBalancer", inventoryClass = LoadBalancerInventory.class,
                foreignKey = "loadBalancerUuid", expandedInventoryKey = "uuid"),
})
public class VirtualRouterLoadBalancerRefInventory {
    private Long id;
    private String virtualRouterVmUuid;
    private String loadBalancerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VirtualRouterLoadBalancerRefInventory valueOf(VirtualRouterLoadBalancerRefVO vo) {
        VirtualRouterLoadBalancerRefInventory inv = new VirtualRouterLoadBalancerRefInventory();
        inv.setId(vo.getId());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setVirtualRouterVmUuid(vo.getVirtualRouterVmUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(inv.getLastOpDate());
        return inv;
    }

    public static List<VirtualRouterLoadBalancerRefInventory> valueOf(Collection<VirtualRouterLoadBalancerRefVO> vos) {
        List<VirtualRouterLoadBalancerRefInventory> invs = new ArrayList<VirtualRouterLoadBalancerRefInventory>();
        for (VirtualRouterLoadBalancerRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
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
