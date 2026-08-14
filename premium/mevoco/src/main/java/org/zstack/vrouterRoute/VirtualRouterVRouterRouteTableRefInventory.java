package org.zstack.vrouterRoute;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 16/06/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VirtualRouterVRouterRouteTableRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vrouterRouteTable", inventoryClass = VRouterRouteTableInventory.class,
                foreignKey = "routeTableUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "virtualRouterVm", inventoryClass = VirtualRouterVmInventory.class,
                foreignKey = "virtualRouterVmUuid", expandedInventoryKey = "uuid"),
})
public class VirtualRouterVRouterRouteTableRefInventory implements Serializable {
    private String virtualRouterVmUuid;

    private String routeTableUuid;

    public VirtualRouterVRouterRouteTableRefInventory() {
    }

    protected VirtualRouterVRouterRouteTableRefInventory(VirtualRouterVRouterRouteTableRefVO vo) {
        this.setRouteTableUuid(vo.getRouteTableUuid());
        this.setVirtualRouterVmUuid(vo.getVirtualRouterVmUuid());
    }

    public static VirtualRouterVRouterRouteTableRefInventory valueOf(VirtualRouterVRouterRouteTableRefVO vo) {
        return new VirtualRouterVRouterRouteTableRefInventory(vo);
    }

    public static List<VirtualRouterVRouterRouteTableRefInventory> valueOf(Collection<VirtualRouterVRouterRouteTableRefVO> vos) {
        List<VirtualRouterVRouterRouteTableRefInventory> invs = new ArrayList<>(vos.size());
        for (VirtualRouterVRouterRouteTableRefVO vo : vos) {
            invs.add(VirtualRouterVRouterRouteTableRefInventory.valueOf(vo));
        }

        return invs;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }
}
