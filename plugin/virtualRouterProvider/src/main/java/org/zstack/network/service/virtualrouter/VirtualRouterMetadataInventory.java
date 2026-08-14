package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VirtualRouterMetadataVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "applianceVm", inventoryClass = ApplianceVmInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "uuid"),
})
public class VirtualRouterMetadataInventory {
    private String uuid;
    private String zvrVersion;
    private String vyosVersion;
    private String kernelVersion;

    public static VirtualRouterMetadataInventory valueOf(VirtualRouterMetadataVO vo){
        VirtualRouterMetadataInventory inv = new VirtualRouterMetadataInventory();
        inv.setUuid(vo.getUuid());
        inv.setZvrVersion(vo.getZvrVersion());
        inv.setVyosVersion(vo.getVyosVersion());
        inv.setKernelVersion(vo.getKernelVersion());
        return inv;
    }

    public static List<VirtualRouterMetadataInventory> valueOf(Collection<VirtualRouterMetadataVO> vos){
        List<VirtualRouterMetadataInventory> invs = new ArrayList<>(vos.size());
        for (VirtualRouterMetadataVO vo : vos) {
            invs.add(VirtualRouterMetadataInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getZvrVersion() {
        return zvrVersion;
    }

    public void setZvrVersion(String zvrVersion) {
        this.zvrVersion = zvrVersion;
    }

    public String getVyosVersion() {
        return vyosVersion;
    }

    public void setVyosVersion(String vyosVersion) {
        this.vyosVersion = vyosVersion;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }
}
