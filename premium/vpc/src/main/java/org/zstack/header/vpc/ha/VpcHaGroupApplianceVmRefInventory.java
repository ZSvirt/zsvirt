package org.zstack.header.vpc.ha;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcHaGroupApplianceVmRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "ha", inventoryClass = VpcHaGroupInventory.class,
                foreignKey = "vpcHaRouterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "uuid"),
})

public class VpcHaGroupApplianceVmRefInventory {
    private String uuid;
    private String vpcHaRouterUuid;

    public static VpcHaGroupApplianceVmRefInventory valueOf(VpcHaGroupApplianceVmRefVO vo) {
        VpcHaGroupApplianceVmRefInventory inv = new VpcHaGroupApplianceVmRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setVpcHaRouterUuid(vo.getVpcHaRouterUuid());
        return inv;
    }

    public static List<VpcHaGroupApplianceVmRefInventory> valueOf(Collection<VpcHaGroupApplianceVmRefVO> vos) {
        List<VpcHaGroupApplianceVmRefInventory> invs = new ArrayList<VpcHaGroupApplianceVmRefInventory>();
        for (VpcHaGroupApplianceVmRefVO vo : vos) {
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

    public String getVpcHaRouterUuid() {
        return vpcHaRouterUuid;
    }

    public void setVpcHaRouterUuid(String vpcHaRouterUuid) {
        this.vpcHaRouterUuid = vpcHaRouterUuid;
    }
}
