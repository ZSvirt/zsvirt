package org.zstack.network.service.virtualrouter.portforwarding;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = VirtualRouterPortForwardingRuleRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "portForwarding", inventoryClass = PortForwardingRuleInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "virtualRouterVmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = ApplianceVmInventory.class, expandedField = "virtualRouterPortforwardingRef", inventoryClass = VirtualRouterPortForwardingRuleRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "virtualRouterVmUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = ApplianceVmInventory.class, alias = "portForwarding", expandedField = "virtualRouterPortforwardingRef.portForwarding")
})
public class VirtualRouterPortForwardingRuleRefInventory {
    private String uuid;
    private String vipUuid;
    private String virtualRouterVmUuid;

    public static VirtualRouterPortForwardingRuleRefInventory valueOf(VirtualRouterPortForwardingRuleRefVO vo) {
        VirtualRouterPortForwardingRuleRefInventory inv = new VirtualRouterPortForwardingRuleRefInventory();
        inv.setVirtualRouterVmUuid(vo.getVirtualRouterVmUuid());
        inv.setUuid(vo.getUuid());
        inv.setVipUuid(vo.getVipUuid());
        return inv;
    }

    public static List<VirtualRouterPortForwardingRuleRefInventory> valueOf(Collection<VirtualRouterPortForwardingRuleRefVO> vos) {
        List<VirtualRouterPortForwardingRuleRefInventory> invs = new ArrayList<VirtualRouterPortForwardingRuleRefInventory>();
        for (VirtualRouterPortForwardingRuleRefVO vo : vos) {
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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
