package org.zstack.network.service.virtualrouter.eip;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = VirtualRouterEipRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "eip", inventoryClass = EipInventory.class,
                foreignKey = "eipUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "virtualRouterVmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = ApplianceVmInventory.class, expandedField = "virtualRouterEipRef", inventoryClass = VirtualRouterEipRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "virtualRouterVmUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = ApplianceVmInventory.class, alias = "eip", expandedField = "virtualRouterEipRef.eip")
})
public class VirtualRouterEipRefInventory {
    private String eipUuid;
    private String virtualRouterVmUuid;

    public static VirtualRouterEipRefInventory valueOf(VirtualRouterEipRefVO vo) {
        VirtualRouterEipRefInventory inv = new VirtualRouterEipRefInventory();
        inv.setEipUuid(vo.getEipUuid());
        inv.setVirtualRouterVmUuid(vo.getVirtualRouterVmUuid());
        return inv;
    }

    public static List<VirtualRouterEipRefInventory> valueOf(Collection<VirtualRouterEipRefVO> vos) {
        List<VirtualRouterEipRefInventory> invs = new ArrayList<VirtualRouterEipRefInventory>();
        for (VirtualRouterEipRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
