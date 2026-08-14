package org.zstack.network.service.virtualrouter.vip;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VirtualRouterVipVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "virtualRouterVmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = ApplianceVmInventory.class, expandedField = "virtualRouterVipRef", inventoryClass = VirtualRouterVipInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "virtualRouterVmUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = ApplianceVmInventory.class, alias = "vip", expandedField = "virtualRouterVipRef.vip")
})
public class VirtualRouterVipInventory {
    private String uuid;
    private String virtualRouterVmUuid;
    
    public static VirtualRouterVipInventory valueOf(VirtualRouterVipVO vo) {
        VirtualRouterVipInventory inv = new VirtualRouterVipInventory();
        inv.setUuid(vo.getUuid());
        inv.setVirtualRouterVmUuid(vo.getVirtualRouterVmUuid());
        return inv;
    }

    public static List<VirtualRouterVipInventory> valueOf(Collection<VirtualRouterVipVO> vos) {
        List<VirtualRouterVipInventory> invs = new ArrayList<VirtualRouterVipInventory>();
        for (VirtualRouterVipVO vo : vos) {
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

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
